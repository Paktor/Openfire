/**
 * $RCSfile  $
 * $Revision  $
 * $Date  $
 *
 * Copyright (C) 1999-2008 Jive Software. All rights reserved.
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
package org.jivesoftware.openfire.stats;

import com.codahale.metrics.Gauge;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static com.codahale.metrics.MetricRegistry.name;
import static org.jivesoftware.util.metric.MetricRegistryFactory.getMetricRegistry;

/**
 * Stores statistics being tracked by the server.
 */
public class StatisticsManager {

    private static StatisticsManager instance = new StatisticsManager();

    public static StatisticsManager getInstance() {
        return instance;
    }

    private final Map<String, Statistic> statistics = new ConcurrentHashMap<String, Statistic>();
    private final Map<String, List<String>> multiStatGroups = new ConcurrentHashMap<String, List<String>>();
    private final Map<String, String> keyToGroupMap = new ConcurrentHashMap<String, String>();

    private StatisticsManager() {
    }

    /**
     * Adds a stat to be tracked to the StatManager.
     *
     * @param statKey the statistic key.
     * @param definition the statistic to be tracked.
     */
    public void addStatistic(String statKey, final Statistic definition) {
        statistics.put(statKey, definition);
        getMetricRegistry().register(
                name(StatisticsManager.class.getSimpleName(), statKey), new Gauge<Integer>() {
                    @Override
                    public Integer getValue() {
                        return (int) definition.sample();
                    }
                });
    }

    /**
     * Returns a statistic being tracked by the StatManager.
     *
     * @param statKey The key of the definition.
     * @return Returns the related stat.
     */
    public Statistic getStatistic(String statKey) {
        return statistics.get(statKey);
    }

    public void addMultiStatistic(String statKey, String groupName, Statistic statistic) {
        addStatistic(statKey, statistic);
        List<String> group = multiStatGroups.get(groupName);
        if(group == null) {
            group = new ArrayList<String>();
            multiStatGroups.put(groupName, group);
        }
        group.add(statKey);
        keyToGroupMap.put(statKey, groupName);
    }

    public List<String> getStatGroup(String statGroup) {
        return multiStatGroups.get(statGroup);
    }

    public String getMultistatGroup(String statKey) {
        return keyToGroupMap.get(statKey);
    }

    /**
     * Returns all statistics that the StatManager is tracking.
     * @return Returns all statistics that the StatManager is tracking.
     */
    public Set<Map.Entry<String, Statistic>> getAllStatistics() {
        return statistics.entrySet();
    }

    /**
     * Removes a statistic from the server.
     *
     * @param statKey The key of the stat to be removed.
     */
    public void removeStatistic(String statKey) {
        statistics.remove(statKey);
        getMetricRegistry().remove(name(StatisticsManager.class.getSimpleName(), statKey));
    }

}