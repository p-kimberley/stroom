/*
 * Copyright 2017 Crown Copyright
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package stroom.query.common.v2;

import stroom.query.api.v2.ResultRequest;
import stroom.query.api.v2.SearchRequest;
import stroom.query.api.v2.TableSettings;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class CoprocessorSettingsFactory {
//    private final Map<String, CoprocessorKey> componentIdToCoprocessorKeyMap = new HashMap<>();
//    private final Map<CoprocessorKey, CoprocessorSettings> map = new HashMap<>();
//
//    private CoprocessorSettingsMap(final Map<String, TableSettings> settingsMap) {
//        // Group common settings.
//        final Map<TableSettings, Set<String>> groupMap = new HashMap<>();
//        for (final Entry<String, TableSettings> entry : settingsMap.entrySet()) {
//            final String componentId = entry.getKey();
//            final TableSettings tableSettings = entry.getValue();
//            if (tableSettings != null) {
//                Set<String> set = groupMap.computeIfAbsent(tableSettings, k -> new HashSet<>());
//                set.add(componentId);
//            }
//        }
//
//        int i = 0;
//        for (final Entry<TableSettings, Set<String>> entry : groupMap.entrySet()) {
//            final TableSettings tableSettings = entry.getKey();
//            final Set<String> componentIds = entry.getValue();
//            final CoprocessorKey key = new CoprocessorKey(i++, componentIds.toArray(new String[componentIds.size()]));
//            map.put(key, new TableCoprocessorSettings(tableSettings));
//            for (String componentId : componentIds) {
//                componentIdToCoprocessorKeyMap.put(componentId, key);
//            }
//        }
//    }

    public static List<CoprocessorSettings> create(final SearchRequest searchRequest) {
        // Group common settings.
        final Map<TableSettings, List<String>> groupMap = new HashMap<>();
        for (final ResultRequest resultRequest : searchRequest.getResultRequests()) {
            final String componentId = resultRequest.getComponentId();
            final TableSettings tableSettings = resultRequest.getMappings().get(0);
            if (tableSettings != null) {
                groupMap.computeIfAbsent(tableSettings, k -> new ArrayList<>()).add(componentId);
            }
        }
        final AtomicInteger atomicInteger = new AtomicInteger();
        return groupMap
                .entrySet()
                .stream()
                .map(e -> {
                            final String key = String.valueOf(atomicInteger.incrementAndGet());
                            return new TableCoprocessorSettings(key, e.getValue(), e.getKey());
                        })
                .collect(Collectors.toList());
    }

//    public static List<CoprocessorSettings> createSettingsList(final List<CoprocessorSettings> settingsList) {
//        return settingsList.stream().map(CoprocessorSet::getSettings).collect(Collectors.toList());
//    }

//    public CoprocessorKey getCoprocessorKey(final String componentId) {
//        return componentIdToCoprocessorKeyMap.get(componentId);
//    }
//
//    public Map<CoprocessorKey, CoprocessorSettings> getMap() {
//        return map;
//    }
}
