/**
 * $Revision: 4005 $
 * $Date: 2006-06-16 08:58:27 -0700 (Fri, 16 Jun 2006) $
 *
 * Copyright (C) 2005-2008 Jive Software. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jivesoftware.util;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.MetricRegistry;
import org.jivesoftware.util.metric.MetricRegistryFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.Map;
import java.util.TimerTask;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static com.codahale.metrics.MetricRegistry.name;
import static java.lang.System.currentTimeMillis;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.jivesoftware.util.JiveGlobals.getIntProperty;

/**
 * Performs tasks using worker threads. It also allows tasks to be scheduled to be
 * run at future dates. This class mimics relevant methods in both
 * {@link ExecutorService} and {@link ScheduledExecutorService}. Any {@link TimerTask} that's
 * scheduled to be run in the future will automatically be run using the thread
 * executor's thread pool. This means that the standard restriction that TimerTasks
 * should run quickly does not apply.
 *
 * @author Matt Tucker
 */
public class TaskEngine {
    private final Logger Log = LoggerFactory.getLogger(TaskEngine.class);

    private static TaskEngine instance = new TaskEngine();

    /**
     * Returns a task engine instance (singleton).
     *
     * @return a task engine.
     */
    public static TaskEngine getInstance() {
        return instance;
    }

    private ScheduledExecutorService scheduledExecutor;
    private ExecutorService executor;
    private Map<TimerTask, ScheduledFuture> futureMap = new ConcurrentHashMap<TimerTask, ScheduledFuture>();
    private Histogram waitInTheQueueHistogram;

    /**
     * Constructs a new task engine.
     */
    private TaskEngine() {
        int nThreads = getIntProperty("xmpp.client.processing.threads", 16) * 2; // incoming connections * 2
        nThreads = getIntProperty("xmpp.server.background.threads", nThreads);   // to override ..processing.threads
        int keepAlive = getIntProperty("xmpp.server.background.keepAlive", 600); // in seconds

        int scheduledThreads = getIntProperty("xmpp.client.processing.scheduled", 30);

        executor = new ThreadPoolExecutor(nThreads, nThreads,
                                          keepAlive, TimeUnit.SECONDS,
                                          new LinkedBlockingQueue<Runnable>(),
                                          new TaskThreadFactory("TaskEngine-pool-"));

        scheduledExecutor = new ScheduledThreadPoolExecutor(scheduledThreads,
                                                            new TaskThreadFactory("TaskEngine-scheduler-"));

        MetricRegistry metricRegistry = MetricRegistryFactory.getMetricRegistry();

        // monitor queues size
        metricRegistry.register(name("TaskEngine", "executorQueue"), new Gauge<Integer>() {
            @Override
            public Integer getValue() {
                return ((ThreadPoolExecutor) executor).getQueue().size();
            }
        });

        metricRegistry.register(name("TaskEngine", "scheduledQueue"), new Gauge<Integer>() {
            @Override
            public Integer getValue() {
                return ((ThreadPoolExecutor) scheduledExecutor).getQueue().size();
            }
        });

        // wait time in the queue for regular one run tasks
        waitInTheQueueHistogram = metricRegistry.histogram(name("TaskEngine", "executorWait"));

        Log.info("TaskEngine was initialized with {} threads, keep alive is set to {} seconds. " +
                         "Scheduled threads amount was set to {}", nThreads, keepAlive, scheduledThreads);

    }

    /**
     * Submits a Runnable task for execution and returns a Future
     * representing that task.
     *
     * @param task the task to submit.
     * @return a Future representing pending completion of the task,
     *      and whose <tt>get()</tt> method will return <tt>null</tt>
     *      upon completion.
     * @throws java.util.concurrent.RejectedExecutionException if task cannot be scheduled
     *      for execution.
     * @throws NullPointerException if task null.
     */
    public Future<?> submit(final Runnable task) {
        return executor.submit(new WaitMonitorTaskWrapper(task));
    }

    /**
     * Schedules the specified task for execution after the specified delay.
     *
     * @param task  task to be scheduled.
     * @param delay delay in milliseconds before task is to be executed.
     * @throws IllegalArgumentException if <tt>delay</tt> is negative, or
     *         <tt>delay + System.currentTimeMillis()</tt> is negative.
     * @throws IllegalStateException if task was already scheduled or
     *         cancelled, or timer was cancelled.
     */
    public void schedule(TimerTask task, long delay) {
        scheduledExecutor.schedule(new TimerTaskWrapper(task), delay, MILLISECONDS);
    }

    /**
     * Schedules the specified task for execution at the specified time.  If
     * the time is in the past, the task is scheduled for immediate execution.
     *
     * @param task task to be scheduled.
     * @param time time at which task is to be executed.
     * @throws IllegalArgumentException if <tt>time.getTime()</tt> is negative.
     * @throws IllegalStateException if task was already scheduled or
     *         cancelled, timer was cancelled, or timer thread terminated.
     */
    public void schedule(TimerTask task, Date time) {
        scheduledExecutor.schedule(new TimerTaskWrapper(task),
                                   time.getTime() - currentTimeMillis(), MILLISECONDS);
    }

    /**
     * Schedules the specified task for repeated <i>fixed-delay execution</i>,
     * beginning after the specified delay.  Subsequent executions take place
     * at approximately regular intervals separated by the specified period.
     *
     * <p>In fixed-delay execution, each execution is scheduled relative to
     * the actual execution time of the previous execution.  If an execution
     * is delayed for any reason (such as garbage collection or other
     * background activity), subsequent executions will be delayed as well.
     * In the long run, the frequency of execution will generally be slightly
     * lower than the reciprocal of the specified period (assuming the system
     * clock underlying <tt>Object.wait(long)</tt> is accurate).
     *
     * <p>Fixed-delay execution is appropriate for recurring activities
     * that require "smoothness."  In other words, it is appropriate for
     * activities where it is more important to keep the frequency accurate
     * in the short run than in the long run.  This includes most animation
     * tasks, such as blinking a cursor at regular intervals.  It also includes
     * tasks wherein regular activity is performed in response to human
     * input, such as automatically repeating a character as long as a key
     * is held down.
     *
     * @param task task to be scheduled.
     * @param delay  delay in milliseconds before task is to be executed.
     * @param period time in milliseconds between successive task executions.
     * @throws IllegalArgumentException if <tt>delay</tt> is negative, or
     *         <tt>delay + System.currentTimeMillis()</tt> is negative.
     * @throws IllegalStateException if task was already scheduled or
     *         cancelled, timer was cancelled, or timer thread terminated.
     */
    public void schedule(TimerTask task, long delay, long period) {
        ScheduledFuture<?> future = scheduledExecutor.scheduleAtFixedRate(
                new TimerTaskWrapper(task), delay, period, MILLISECONDS);
        futureMap.put(task, future);
    }

    /**
     * Schedules the specified task for repeated <i>fixed-delay execution</i>,
     * beginning at the specified time. Subsequent executions take place at
     * approximately regular intervals, separated by the specified period.
     *
     * <p>In fixed-delay execution, each execution is scheduled relative to
     * the actual execution time of the previous execution.  If an execution
     * is delayed for any reason (such as garbage collection or other
     * background activity), subsequent executions will be delayed as well.
     * In the long run, the frequency of execution will generally be slightly
     * lower than the reciprocal of the specified period (assuming the system
     * clock underlying <tt>Object.wait(long)</tt> is accurate).
     *
     * <p>Fixed-delay execution is appropriate for recurring activities
     * that require "smoothness."  In other words, it is appropriate for
     * activities where it is more important to keep the frequency accurate
     * in the short run than in the long run.  This includes most animation
     * tasks, such as blinking a cursor at regular intervals.  It also includes
     * tasks wherein regular activity is performed in response to human
     * input, such as automatically repeating a character as long as a key
     * is held down.
     *
     * @param task task to be scheduled.
     * @param firstTime First time at which task is to be executed.
     * @param period time in milliseconds between successive task executions.
     * @throws IllegalArgumentException if <tt>time.getTime()</tt> is negative.
     * @throws IllegalStateException if task was already scheduled or
     *         cancelled, timer was cancelled, or timer thread terminated.
     */
    public void schedule(TimerTask task, Date firstTime, long period) {
        ScheduledFuture<?> future = scheduledExecutor.scheduleAtFixedRate(
                new TimerTaskWrapper(task), firstTime.getTime() - currentTimeMillis(), period, MILLISECONDS);
        futureMap.put(task, future);
    }

    /**
     * Schedules the specified task for repeated <i>fixed-rate execution</i>,
     * beginning after the specified delay.  Subsequent executions take place
     * at approximately regular intervals, separated by the specified period.
     *
     * <p>In fixed-rate execution, each execution is scheduled relative to the
     * scheduled execution time of the initial execution.  If an execution is
     * delayed for any reason (such as garbage collection or other background
     * activity), two or more executions will occur in rapid succession to
     * "catch up."  In the long run, the frequency of execution will be
     * exactly the reciprocal of the specified period (assuming the system
     * clock underlying <tt>Object.wait(long)</tt> is accurate).
     *
     * <p>Fixed-rate execution is appropriate for recurring activities that
     * are sensitive to <i>absolute</i> time, such as ringing a chime every
     * hour on the hour, or running scheduled maintenance every day at a
     * particular time.  It is also appropriate for recurring activities
     * where the total time to perform a fixed number of executions is
     * important, such as a countdown timer that ticks once every second for
     * ten seconds.  Finally, fixed-rate execution is appropriate for
     * scheduling multiple repeating timer tasks that must remain synchronized
     * with respect to one another.
     *
     * @param task task to be scheduled.
     * @param delay  delay in milliseconds before task is to be executed.
     * @param period time in milliseconds between successive task executions.
     * @throws IllegalArgumentException if <tt>delay</tt> is negative, or
     *         <tt>delay + System.currentTimeMillis()</tt> is negative.
     * @throws IllegalStateException if task was already scheduled or
     *         cancelled, timer was cancelled, or timer thread terminated.
     */
    public void scheduleAtFixedRate(TimerTask task, long delay, long period) {
        schedule(task, delay, period);
    }

    /**
     * Schedules the specified task for repeated <i>fixed-rate execution</i>,
     * beginning at the specified time. Subsequent executions take place at
     * approximately regular intervals, separated by the specified period.
     *
     * <p>In fixed-rate execution, each execution is scheduled relative to the
     * scheduled execution time of the initial execution.  If an execution is
     * delayed for any reason (such as garbage collection or other background
     * activity), two or more executions will occur in rapid succession to
     * "catch up."  In the long run, the frequency of execution will be
     * exactly the reciprocal of the specified period (assuming the system
     * clock underlying <tt>Object.wait(long)</tt> is accurate).
     *
     * <p>Fixed-rate execution is appropriate for recurring activities that
     * are sensitive to <i>absolute</i> time, such as ringing a chime every
     * hour on the hour, or running scheduled maintenance every day at a
     * particular time.  It is also appropriate for recurring activities
     * where the total time to perform a fixed number of executions is
     * important, such as a countdown timer that ticks once every second for
     * ten seconds.  Finally, fixed-rate execution is appropriate for
     * scheduling multiple repeating timer tasks that must remain synchronized
     * with respect to one another.
     *
     * @param task task to be scheduled.
     * @param firstTime First time at which task is to be executed.
     * @param period time in milliseconds between successive task executions.
     * @throws IllegalArgumentException if <tt>time.getTime()</tt> is negative.
     * @throws IllegalStateException if task was already scheduled or
     *         cancelled, timer was cancelled, or timer thread terminated.
     */
    public void scheduleAtFixedRate(TimerTask task, Date firstTime, long period) {
        schedule(task, firstTime, period);
    }

    /**
     * Cancels the execution of a scheduled task. {@link java.util.TimerTask#cancel()}
     *
     * @param task the scheduled task to cancel.
     */
    public void cancelScheduledTask(TimerTask task) {
        ScheduledFuture<?> future = futureMap.remove(task);
        if (future != null) {
            future.cancel(false);
        }
    }

    /**
     * Shuts down the task engine service.
     */
    public void shutdown() {
        if (executor != null) {
            executor.shutdown();
            executor = null;
        }

        if (scheduledExecutor != null) {
            scheduledExecutor.shutdown();
            scheduledExecutor = null;
        }
    }

    /**
     * Wrapper class for a standard TimerTask. It simply executes the TimerTask
     * using the executor's thread pool.
     */
    private class TimerTaskWrapper extends TimerTask {

        private TimerTask task;

        public TimerTaskWrapper(TimerTask task) {
            this.task = task;
        }

        @Override
		public void run() {
            try {
                task.run();
            } catch (Exception e) {
                // swallow error to do not prevent future scheduled task execution (scheduler will freeze it otherwise)
                Log.error("Unhandled exception in scheduled {}", task.getClass().getName(), e);
            }
        }
    }

    /**
     * Wrapper class for monitoring a time spent in the queue before actual execution
     */
    private class WaitMonitorTaskWrapper implements Runnable {
        private long start = currentTimeMillis();
        private Runnable target;

        public WaitMonitorTaskWrapper(Runnable target) {
            this.target = target;
        }

        @Override
		public void run() {
            try {
                waitInTheQueueHistogram.update(currentTimeMillis() - start);
                target.run();
            } catch (Exception e) {
                Log.error("Unhandled exception in {}", target.getClass().getName(), e);
            }
        }
    }

    public static class TaskThreadFactory implements ThreadFactory {
        private final String prefix;
        private final AtomicInteger threadNumber = new AtomicInteger(1);

        public TaskThreadFactory(String prefix) {
            this.prefix = prefix;
        }

        public Thread newThread(Runnable runnable) {
            // Use our own naming scheme for the threads.
            Thread thread = new Thread(Thread.currentThread().getThreadGroup(), runnable,
                                       prefix + threadNumber.getAndIncrement(), 0);
            // Make workers daemon threads.
            thread.setDaemon(true);
            if (thread.getPriority() != Thread.NORM_PRIORITY) {
                thread.setPriority(Thread.NORM_PRIORITY);
            }
            return thread;
        }
    };

}