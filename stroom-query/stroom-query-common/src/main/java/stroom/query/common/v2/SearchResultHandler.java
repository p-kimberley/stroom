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

import stroom.query.api.v2.TableSettings;
import stroom.search.coprocessor.FieldUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SearchResultHandler implements ResultHandler {
    private final Map<String, TablePayloadHandler> handlerMap;
    private final Map<String, TableDataFactory> componentMap;

    public SearchResultHandler(final List<CoprocessorSettings> settingsList,
                               final Sizes defaultMaxResultsSizes,
                               final Sizes storeSize,
                               final Map<String, String> paramMap) {
        handlerMap = new HashMap<>();
        componentMap = new HashMap<>();

        settingsList.forEach(settings -> {
            if (settings instanceof TableCoprocessorSettings) {
                final TableCoprocessorSettings tableCoprocessorSettings = (TableCoprocessorSettings) settings;
                final TableSettings tableSettings = tableCoprocessorSettings.getTableSettings();
                // Create a set of sizes that are the minimum values for the combination of user provided sizes for the
                // table and the default maximum sizes.
                final Sizes maxResults = Sizes.min(Sizes.create(tableSettings.getMaxResults()), defaultMaxResultsSizes);
                final String[] fieldNames = FieldUtil.getFieldNames(tableCoprocessorSettings.getFields());

                final TableDataFactory tableDataFactory = new TableDataFactory(
                        tableSettings,
                        fieldNames,
                        paramMap,
                        maxResults,
                        storeSize);

                final ValArraySerde valArraySerde = ValArraySerde.create(tableCoprocessorSettings.getFields());

                final TablePayloadHandler tablePayloadHandler = new TablePayloadHandler(valArraySerde, tableDataFactory);
                handlerMap.put(tableCoprocessorSettings.getCoprocessorId(), tablePayloadHandler);
                tableCoprocessorSettings.getComponentIdList().forEach(componentId ->
                        componentMap.put(componentId, tableDataFactory));
            }
        });
    }

    @Override
    public void handle(final List<Payload> payloads) {
        if (payloads != null) {
            for (final Payload payload : payloads) {
                if (payload instanceof TablePayload) {
                    final TablePayload tablePayload = (TablePayload) payload;
                    final TablePayloadHandler payloadHandler = handlerMap.get(payload.getKey());
                    payloadHandler.process(tablePayload);
                }
            }
        }
    }

    @Override
    public Data getResultStore(final String componentId) {
        final TableDataFactory tableDataFactory = componentMap.get(componentId);
        if (tableDataFactory != null) {
            return tableDataFactory.getData();
        }
        return null;
    }
}
