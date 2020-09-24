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

import stroom.dashboard.expression.v1.Generator;
import stroom.query.api.v2.Field;
import stroom.query.util.LambdaLogger;
import stroom.query.util.LambdaLoggerFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

public class TablePayloadHandler implements PayloadHandler {
    private final Map<GroupKey, Item> groupingMap = new ConcurrentHashMap<>();
    private final Map<GroupKey, Items> childMap = new ConcurrentHashMap<>();

    private static final Logger LOGGER = LoggerFactory.getLogger(TablePayloadHandler.class);
    private static final LambdaLogger LAMBDA_LOGGER = LambdaLoggerFactory.getLogger(TablePayloadHandler.class);

    private final CompiledSorter compiledSorter;
    private final CompiledDepths compiledDepths;
    private final Sizes maxResults;
    private final Sizes storeSize;
    private final AtomicLong totalResultCount = new AtomicLong();
    private final AtomicLong resultCount = new AtomicLong();

    private volatile Data data;
    private volatile boolean hasEnoughData;

    public TablePayloadHandler(final List<Field> fields,
                               final boolean showDetails,
                               final Sizes maxResults,
                               final Sizes storeSize) {
        this.compiledSorter = new CompiledSorter(fields);
        this.maxResults = maxResults;
        this.storeSize = storeSize;
        this.compiledDepths = new CompiledDepths(fields, showDetails);
        this.data = new Data(Collections.emptyMap(), 0, 0);
    }

    void clear() {
        totalResultCount.set(0);
        data = new Data(Collections.emptyMap(), 0, 0);
    }

    void addQueue(final List<Item> newQueue) {
        LAMBDA_LOGGER.trace(() -> LambdaLogger.buildMessage("addQueue called for {} items", newQueue.size()));
        if (newQueue != null) {
            if (!Thread.currentThread().isInterrupted() && !hasEnoughData) {
                // Add the new queue to the pending merge queue ready for
                // merging.
                try {
                    mergeQueue(newQueue);
                } catch (final RuntimeException e) {
                    LOGGER.error(e.getMessage(), e);
                }
            }
        }

        LAMBDA_LOGGER.trace(() -> "Finished adding items to the queue");
    }

    private void mergeQueue(final List<Item> newQueue) {
        // Update the total number of results that we have received.
        totalResultCount.getAndAdd(newQueue.size());

        newQueue.forEach(item -> {
            final GroupKey key = item.getKey();
            if (key != null && key.getValues() != null) {
                groupingMap.compute(key, (k, v) -> {
                    Item result = v;

                    // Items with a null key values will not undergo partitioning and reduction as we don't want to
                    // group items with null key values as they are child items.
                    if (result == null) {
                        final boolean success = add(item);
                        if (success) {
                            result = item;
                        }
                    } else {
                        // Combine the new item into the original item.
                        for (int i = 0; i < compiledDepths.getDepths().length; i++) {
                            result.generators[i] = combine(compiledDepths.getDepths()[i], compiledDepths.getMaxDepth(), result.generators[i], item.generators[i], item.depth);
                        }
                    }

                    return result;
                });
            } else {
                add(item);
            }
        });

        // Create new data.
        this.data = new Data(childMap, resultCount.get(), totalResultCount.get());

        // Some searches can be terminated early if the user is not sorting or grouping.
        if (!hasEnoughData && !compiledSorter.hasSort() && !compiledDepths.hasGroupBy()) {
            // No sorting or grouping so we can stop the search as soon as we have the number of results requested by
            // the client
            if (maxResults != null && totalResultCount.get() >= maxResults.size(0)) {
                hasEnoughData = true;
            }
        }
    }

    private boolean add(final Item item) {
        final AtomicBoolean success = new AtomicBoolean();

        GroupKey parentKey;
        if (item.getKey() != null && item.getKey().getParent() != null) {
            parentKey = item.getKey().getParent();
        } else {
            parentKey = Data.ROOT_KEY;
        }

        final AtomicReference<GroupKey> removalKey = new AtomicReference<>();
        childMap.compute(parentKey, (k, v) -> {
            Items result = v;

            if (result == null) {
                result = new ItemsList(Collections.synchronizedList(new ArrayList<>()));
                result.add(item);
                resultCount.incrementAndGet();
                success.set(true);

            } else {
                final List<Item> list = ((ItemsList) result).list;
                final int maxSize = storeSize.size(item.depth);

                if (compiledSorter.hasSort()) {
                    int pos = Collections.binarySearch(list, item, compiledSorter);
                    if (pos < 0) {
                        // It isn't already present so insert.
                        pos = Math.abs(pos + 1);
                    }
                    if (pos < maxSize) {
                        list.add(pos, item);
                        resultCount.incrementAndGet();
                        success.set(true);

                        if (list.size() > maxSize) {
                            // Remove the end.
                            final Item removed = list.remove(list.size() - 1);
                            // We removed an item so record that we need to cascade the removal.
                            removalKey.set(removed.key);
                        }
                    } else {
                        // We didn't add so record that we need to remove.
                        removalKey.set(item.key);
                    }

                } else if (result.size() < maxSize) {
                    list.add(item);
                    resultCount.incrementAndGet();
                    success.set(true);

                } else {
                    // We didn't add so record that we need to remove.
                    removalKey.set(item.key);
                }
            }

            return result;
        });

        remove(removalKey.get());
        return success.get();
    }

    private void remove(final GroupKey groupKey) {
        if (groupKey != null) {
            final Items items = childMap.remove(groupKey);
            if (items != null) {
                resultCount.addAndGet(-items.size());
                items.forEach(item -> remove(item.getKey()));
            }
        }
    }

    private Generator combine(final int groupDepth, final int maxDepth, final Generator existingValue,
                              final Generator addedValue, final int depth) {
        Generator output = null;

        if (maxDepth >= depth) {
            if (existingValue != null && addedValue != null) {
                existingValue.merge(addedValue);
                output = existingValue;
            } else if (groupDepth >= 0 && groupDepth <= depth) {
                // This field is grouped so output existing as it must match the
                // added value.
                output = existingValue;
            }
        } else {
            // This field is not grouped so output existing.
            output = existingValue;
        }

        return output;
    }

    public Data getData() {
        return data;
    }

    public static class ItemsList implements Items {
        private final List<Item> list;

        public ItemsList(final List<Item> list) {
            this.list = list;
        }

        @Override
        public boolean add(final Item item) {
            return list.add(item);
        }

        @Override
        public boolean remove(final Item item) {
            return list.remove(item);
        }

        @Override
        public int size() {
            return list.size();
        }

        @Override
        public void sort(final Comparator<Item> comparator) {
        }

        @Override
        public void sortAndTrim(final int size, final Comparator<Item> comparator, final RemoveHandler removeHandler) {
        }

        @Override
        public Iterator<Item> iterator() {
            return list.iterator();
        }
    }
}
