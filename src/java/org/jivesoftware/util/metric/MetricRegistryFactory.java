package org.jivesoftware.util.metric;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Slf4jReporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

import java.util.logging.Level;

import static com.codahale.metrics.Slf4jReporter.forRegistry;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * Provides server metric registry
 *
 * @author Konstantin Yakimov <a href="mailto:kiakimov@gmail.com>kiakimov@gmail.com</a>
 */
public class MetricRegistryFactory {
    private static final Logger log = LoggerFactory.getLogger(MetricRegistryFactory.class);

    private static final MetricRegistry metricRegistry = new MetricRegistry();

    static {
        final Slf4jReporter reporter = forRegistry(metricRegistry)
                .convertRatesTo(SECONDS)
                .convertDurationsTo(MILLISECONDS)
                .build();
        reporter.start(5, MINUTES);

        // jul reset
        SLF4JBridgeHandler.removeHandlersForRootLogger();
        SLF4JBridgeHandler.install();
        java.util.logging.Logger.getLogger("").setLevel(Level.INFO);
   }


    public static MetricRegistry getMetricRegistry() {
        return metricRegistry;
    }

}
