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

import stroom.dashboard.expression.v1.FieldIndexMap;
import stroom.dashboard.expression.v1.Val;
import stroom.query.api.v2.Field;
import stroom.query.api.v2.TableSettings;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;

public class TableCoprocessor implements Coprocessor {
    private static final int DEFAULT_QUEUE_CAPACITY = 1000000;

    private final String key;
    private final LinkedBlockingQueue<Item> queue;
    private final ItemMapper mapper;

    public TableCoprocessor(final TableCoprocessorSettings settings,
                            final FieldIndexMap fieldIndexMap,
                            final Map<String, String> paramMap) {
        final TableSettings tableSettings = settings.getTableSettings();

        final List<Field> fields = tableSettings.getFields();
        final CompiledDepths compiledDepths = new CompiledDepths(fields, tableSettings.showDetail());
        final CompiledFields compiledFields = new CompiledFields(fields, fieldIndexMap, paramMap);

        key = settings.getKey();
        queue = new LinkedBlockingQueue<>(DEFAULT_QUEUE_CAPACITY);
        mapper = new ItemMapper(compiledFields, compiledDepths.getMaxDepth(), compiledDepths.getMaxGroupDepth());
    }

    @Override
    public void receive(final Val[] values) {
        mapper.map(values, item -> {
            try {
                queue.put(item);
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
    }

    @Override
    public Payload createPayload() {
        final List<Item> outputQueue = new ArrayList<>();
        queue.drainTo(outputQueue);
        // Don't create a payload if the queue is empty.
        if (outputQueue.size() == 0) {
            return null;
        }

        return new TablePayload(key, outputQueue);
    }
}
