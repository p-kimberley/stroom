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
import stroom.mapreduce.v2.Pair;
import stroom.mapreduce.v2.UnsafePairQueue;
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
//    private static final Generator[] PARENT_GENERATORS = new Generator[0];

    private final Map<GroupKey, Item> groupingMap = new ConcurrentHashMap<>();
    private final Map<GroupKey, Items<Item>> childMap = new ConcurrentHashMap<>();

//        private final HasTerminate hasTerminate;
//    private final CompiledFields compiledFields;
//
//    private static final int MAX_SIZE = 1000000;


//    private final int maxDepth;
//    private final int maxGroupDepth;


    private static final Logger LOGGER = LoggerFactory.getLogger(TablePayloadHandler.class);
    private static final LambdaLogger LAMBDA_LOGGER = LambdaLoggerFactory.getLogger(TablePayloadHandler.class);

    private final CompiledSorter compiledSorter;
    private final CompiledDepths compiledDepths;
    private final Sizes maxResults;
    private final Sizes storeSize;
    private final AtomicLong totalResultCount = new AtomicLong();
    private final AtomicLong resultCount = new AtomicLong();

    //    private volatile PairQueue<GroupKey, Item> currentQueue;
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
        this.data = new Data(Collections.emptyMap(), 0, 0);//new ResultStoreCreator(compiledSorter).create(0, 0);
    }

    void clear() {
        totalResultCount.set(0);
//        currentQueue = null;
        data = new Data(Collections.emptyMap(), 0, 0);//new ResultStoreCreator(compiledSorter).create(0, 0);
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

//        final PairQueue<GroupKey, Item> outputQueue = new UnsafePairQueue<>();
//
//        /*
//         * Create a partitioner to perform result reduction if needed.
//         */
//        final ItemPartitioner partitioner = new ItemPartitioner(compiledDepths.getDepths(),
//                compiledDepths.getMaxDepth());
//        partitioner.setOutputCollector(outputQueue);
//
//        if (currentQueue != null) {
//            /* First deal with the current queue. */
//            partitioner.read(currentQueue);
//        }
//
//        /* New deal with the new queue. */
//        partitioner.read(newQueue);
//
//        /* Perform partitioning and reduction. */
//        partitioner.partition();
//
//        currentQueue = updateResultStore(outputQueue);


//        final Set<GroupKey> removalSet = Collections.newSetFromMap(new ConcurrentHashMap<>());

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


//        // Cascade removal of items we couldn't all or overflowed max size.
//        removalSet.forEach(this::remove);

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
            Items<Item> result = v;

            if (result == null) {
                result = new ItemsList<>(Collections.synchronizedList(new ArrayList<>()));
                result.add(item);
                resultCount.incrementAndGet();
                success.set(true);

            } else {
                final List<Item> list = ((ItemsList<Item>) result).list;
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
            final Items<Item> items = childMap.remove(groupKey);
            if (items != null) {
                resultCount.addAndGet(-items.size());
                items.forEach(item -> remove(item.getKey()));
            }
        }
    }


//`
//
//    public void sortAndTrim(final Sizes storeSize) {
//        sortAndTrim(storeSize, null, 0);
//    }
//
//    private void sortAndTrim(final Sizes storeSize, final GroupKey parentKey, final int depth) {
//        final Items<Item> parentItems = childMap.get(parentKey);
//        if (parentItems != null) {
//
//            if (storeSize == null) {
//                // no store limits so just sort
//                parentItems.sort(sorter);
//            } else {
//                // sort then trim
//                parentItems.sortAndTrim(storeSize.size(depth), sorter, item -> {
//                    // If there is a group key then cascade removal.
//                    if (item.key != null) {
//                        remove(item.key);
//                    }
//                });
//            }
//
//            // Ensure remaining items children are also trimmed by cascading
//            // trim operation.
//
//            // Lower levels of results should be reduced by increasing
//            // amounts so that we don't get an exponential number of
//            // results.
//            // int sz = size / 10;
//            // if (sz < 1) {
//            // sz = 1;
//            // }
//            for (final Item item : parentItems) {
//                if (item.key != null) {
//                    sortAndTrim(storeSize, item.key, depth + 1);
//                }
//            }
//        }
//    }
//
//    private void remove(final GroupKey parentKey) {
//        final Items<Item> items = childMap.get(parentKey);
//        if (items != null) {
//            childMap.remove(parentKey);
//
//            // Cascade delete.
//            for (final Item item : items) {
//                if (item.key != null) {
//                    remove(item.key);
//                }
//            }
//        }
//    }


//    ublic void map(final GroupKey key, final Val[] values, final OutputCollector<GroupKey, Item> output) {
//        // Add the item to the output recursively up to the max depth.
//        addItem(values, null, PARENT_GENERATORS, 0,compiledDepths.getMaxDepth(), compiledDepths.getMaxGroupDepth(), output);
//    }
//
//    private void map(final Val[] values, final GroupKey parentKey, final Generator[] parentGenerators,
//                     final int depth, final int maxDepth, final int maxGroupDepth, final OutputCollector<GroupKey, Item> output) {
//        // Process list into fields.
//        final Generator[] generators = new Generator[compiledFields.size()];
//
//        List<Val> groupValues = null;
//        int pos = 0;
//        for (final CompiledField compiledField : compiledFields) {
//            Val value = null;
//
//            final Expression expression = compiledField.getExpression();
//            if (expression != null) {
//                final Generator generator = expression.createGenerator();
//                generator.set(values);
//
//                // Only output a value if we are at the group depth or greater
//                // for this field, or have a function.
//                // If we are applying any grouping then maxDepth will be >= 0.
//                if (maxGroupDepth >= depth) {
//                    // We always want to output fields that have an aggregate
//                    // function or fields that are grouped at the current depth
//                    // or above.
//                    if (expression.hasAggregate()
//                            || (compiledField.getGroupDepth() >= 0 && compiledField.getGroupDepth() <= depth)) {
//                        // This field is grouped so output.
//                        generators[pos] = generator;
//                    }
//                } else {
//                    // This field is not grouped so output.
//                    generators[pos] = generator;
//                }
//
//                if (compiledField.getCompiledFilter() != null || compiledField.getGroupDepth() == depth) {
//                    // If we are filtering then we need to evaluate this field
//                    // now so that we can filter the resultant value.
//                    value = generator.eval();
//
//                    if (compiledField.getCompiledFilter() != null && value != null && !compiledField.getCompiledFilter().match(value.toString())) {
//                        // We want to exclude this item.
//                        return;
//                    }
//                }
//            }
//
//            // If this field is being grouped at this depth then add the value
//            // to the group key for this depth.
//            if (compiledField.getGroupDepth() == depth) {
//                if (groupValues == null) {
//                    groupValues = new ArrayList<>();
//                }
//                groupValues.add(value);
//            }
//
//            pos++;
//        }
//
//        // Are we grouping this item?
//        GroupKey key = ROOT_KEY;
//        if (parentKey != null || groupValues != null) {
//            key = new GroupKey(parentKey, groupValues);
//        }
//
//        // If the popToWhenComplete row has child group key sets then add this child group
//        // key to them.
//        for (final Generator parent : parentGenerators) {
//            if (parent != null) {
//                parent.addChildKey(key);
//            }
//        }
//
//        // Add the new item.
//        output.collect(key, new Item(key, generators, depth));
//
//        // If we haven't reached the max depth then recurse.
//        if (depth < maxDepth) {
//            map(values, key, generators, depth + 1, maxDepth, maxGroupDepth, output);
//        }
//    }
//
//    private Item reduce(final Item item1, final Item item2) {
//        // Combine the new item into the original item.
//        for (int i = 0; i < compiledDepths.getDepths().length; i++) {
//            item1.generators[i] = combine(compiledDepths.getDepths()[i], compiledDepths.getMaxDepth(), item1.generators[i], item2.generators[i], item2.depth);
//        }
//        return item1;
//    }

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


//    private PairQueue<GroupKey, Item> updateResultStore(final PairQueue<GroupKey, Item> queue) {
//        // Stick the new reduced results into a new result store.
//        final ResultStoreCreator resultStoreCreator = new ResultStoreCreator(compiledSorter);
//        resultStoreCreator.read(queue);
//
//        // Trim the number of results in the store.
//        resultStoreCreator.sortAndTrim(storeSize);
//
//        // Put the remaining items into the current queue ready for the next
//        // result.
//        final PairQueue<GroupKey, Item> remaining = new UnsafePairQueue<>();
//        long size = 0;
//        for (final Items<Item> items : resultStoreCreator.getChildMap().values()) {
//            for (final Item item : items) {
//                remaining.collect(item.key, item);
//                size++;
//            }
//        }
//
//        // Update the result store reference to point at this new store.
//        this.data = resultStoreCreator.create(size, totalResultCount.get());
//
//        // Some searches can be terminated early if the user is not sorting or grouping.
//        if (!hasEnoughData && !compiledSorter.hasSort() && !compiledDepths.hasGroupBy()) {
//            // No sorting or grouping so we can stop the search as soon as we have the number of results requested by
//            // the client
//            if (maxResults != null && data.getTotalSize() >= maxResults.size(0)) {
//                hasEnoughData = true;
//            }
//        }
//
//        // Give back the remaining queue items ready for the next result.
//        return remaining;
//    }

    public Data getData() {
        return data;
    }

    public static class ItemsList<E> implements Items<E> {
        private final List<E> list;

        public ItemsList(final List<E> list) {
            this.list = list;
        }

        @Override
        public boolean add(final E item) {
            return list.add(item);
        }

        @Override
        public boolean remove(final E item) {
            return list.remove(item);
        }

        @Override
        public int size() {
            return list.size();
        }

        @Override
        public void sort(final Comparator<E> comparator) {
        }

        @Override
        public void sortAndTrim(final int size, final Comparator<E> comparator, final RemoveHandler<E> removeHandler) {
        }

        @Override
        public Iterator<E> iterator() {
            return list.iterator();
        }
    }
}
