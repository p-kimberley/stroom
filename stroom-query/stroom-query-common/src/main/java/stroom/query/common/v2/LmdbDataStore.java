/*
 * Copyright 2018 Crown Copyright
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
 *
 */

package stroom.query.common.v2;

import stroom.dashboard.expression.v1.Any.AnySelector;
import stroom.dashboard.expression.v1.Bottom.BottomSelector;
import stroom.dashboard.expression.v1.Expression;
import stroom.dashboard.expression.v1.FieldIndex;
import stroom.dashboard.expression.v1.Generator;
import stroom.dashboard.expression.v1.Last.LastSelector;
import stroom.dashboard.expression.v1.Nth.NthSelector;
import stroom.dashboard.expression.v1.Selection;
import stroom.dashboard.expression.v1.Selector;
import stroom.dashboard.expression.v1.Top.TopSelector;
import stroom.dashboard.expression.v1.Val;
import stroom.dashboard.expression.v1.ValNull;
import stroom.dashboard.expression.v1.ValSerialiser;
import stroom.pipeline.refdata.util.ByteBufferPool;
import stroom.pipeline.refdata.util.ByteBufferUtils;
import stroom.pipeline.refdata.util.PooledByteBuffer;
import stroom.query.api.v2.TableSettings;
import stroom.util.logging.LambdaLogger;
import stroom.util.logging.LambdaLoggerFactory;

import com.esotericsoftware.kryo.io.ByteBufferOutput;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import org.lmdbjava.CursorIterable;
import org.lmdbjava.CursorIterable.KeyVal;
import org.lmdbjava.Dbi;
import org.lmdbjava.KeyRange;
import org.lmdbjava.Txn;

import java.io.ByteArrayInputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import javax.annotation.Nonnull;

public class LmdbDataStore implements DataStore {

    private static final LambdaLogger LOGGER = LambdaLoggerFactory.getLogger(LmdbDataStore.class);
    private static final long COMMIT_FREQUENCY_MS = 1000;
    private static RawKey ROOT_RAW_KEY;
    private final LmdbEnvironment lmdbEnvironment;
    private final LmdbConfig lmdbConfig;
    private final Dbi<ByteBuffer> lmdbDbi;
    private final ByteBufferPool byteBufferPool;

    private final CompiledField[] compiledFields;
    private final CompiledSorter<HasGenerators>[] compiledSorters;
    private final CompiledDepths compiledDepths;
    private final Sizes maxResults;
    private final AtomicLong totalResultCount = new AtomicLong();
    private final AtomicLong resultCount = new AtomicLong();
    private final ItemSerialiser itemSerialiser;
    private final boolean hasSort;

    private final KeySerde keySerde;
    private final ValueSerde valueSerde;
    private final AtomicBoolean hasEnoughData = new AtomicBoolean();
    private final AtomicBoolean drop = new AtomicBoolean();
    private final AtomicBoolean dropped = new AtomicBoolean();

    private final AtomicBoolean createPayload = new AtomicBoolean();
    private final AtomicReference<byte[]> currentPayload = new AtomicReference<>();

    private final LinkedBlockingQueue<Optional<QueueItem>> queue = new LinkedBlockingQueue<>(1000000);

    private final CountDownLatch addedData;

    LmdbDataStore(final LmdbEnvironment lmdbEnvironment,
                  final LmdbConfig lmdbConfig,
                  final ByteBufferPool byteBufferPool,
                  final String queryKey,
                  final String componentId,
                  final TableSettings tableSettings,
                  final FieldIndex fieldIndex,
                  final Map<String, String> paramMap,
                  final Sizes maxResults,
                  final Sizes storeSize) {
        this.lmdbEnvironment = lmdbEnvironment;
        this.lmdbConfig = lmdbConfig;
        this.maxResults = maxResults;

        compiledFields = CompiledFields.create(tableSettings.getFields(), fieldIndex, paramMap);
        compiledDepths = new CompiledDepths(compiledFields, tableSettings.showDetail());
        compiledSorters = CompiledSorter.create(compiledDepths.getMaxDepth(), compiledFields);

        itemSerialiser = new ItemSerialiser(compiledFields);
        if (ROOT_RAW_KEY == null) {
            ROOT_RAW_KEY = itemSerialiser.toRawKey(Key.root());
        }

        keySerde = new KeySerde(itemSerialiser);
        valueSerde = new ValueSerde(itemSerialiser);

        this.lmdbDbi = lmdbEnvironment.openDbi(queryKey, UUID.randomUUID().toString());
        this.byteBufferPool = byteBufferPool;

        // Find out if we have any sorting.
        boolean hasSort = false;
        for (final CompiledSorter<HasGenerators> sorter : compiledSorters) {
            if (sorter != null) {
                hasSort = true;
                break;
            }
        }
        this.hasSort = hasSort;

        // Start transfer loop.
        addedData = new CountDownLatch(1);
        // TODO : Use provided executor but don't allow it to be terminated by search termination.
        final Executor executor = Executors.newSingleThreadExecutor();
        executor.execute(this::transfer);
    }

    private void commit(final Txn<ByteBuffer> writeTxn) {
        Metrics.measure("Commit", () -> {
            writeTxn.commit();
            writeTxn.close();
        });
    }

    @Override
    public CompletionState getCompletionState() {
        return new CompletionState() {
            @Override
            public void complete() {
                try {
                    queue.put(Optional.empty());
                } catch (final InterruptedException e) {
                    LOGGER.debug(e.getMessage(), e);
                    Thread.currentThread().interrupt();
                    addedData.countDown();
                }
            }

            @Override
            public boolean isComplete() {
                boolean complete = true;

                try {
                    complete = addedData.await(0, TimeUnit.MILLISECONDS);
                } catch (final InterruptedException e) {
                    LOGGER.debug(e.getMessage(), e);
                    Thread.currentThread().interrupt();
                }
                return complete;
            }

            @Override
            public void awaitCompletion() throws InterruptedException {
                addedData.await();
            }

            @Override
            public boolean awaitCompletion(final long timeout, final TimeUnit unit) throws InterruptedException {
                return addedData.await(timeout, unit);
            }

            @Override
            public void accept(final Long value) {
                complete();
            }
        };
    }

    @Override
    public void add(final Val[] values) {
        final int[] groupSizeByDepth = compiledDepths.getGroupSizeByDepth();
        final boolean[][] groupIndicesByDepth = compiledDepths.getGroupIndicesByDepth();
        final boolean[][] valueIndicesByDepth = compiledDepths.getValueIndicesByDepth();

        Key key = Key.root();

        for (int depth = 0; depth < groupIndicesByDepth.length; depth++) {
            final Generator[] generators = new Generator[compiledFields.length];

            final int groupSize = groupSizeByDepth[depth];
            final boolean[] groupIndices = groupIndicesByDepth[depth];
            final boolean[] valueIndices = valueIndicesByDepth[depth];

            Val[] groupValues = ValSerialiser.EMPTY_VALUES;
            if (groupSize > 0) {
                groupValues = new Val[groupSize];
            }

            int groupIndex = 0;
            for (int fieldIndex = 0; fieldIndex < compiledFields.length; fieldIndex++) {
                final CompiledField compiledField = compiledFields[fieldIndex];

                final Expression expression = compiledField.getExpression();
                if (expression != null) {
                    Generator generator = null;
                    Val value = null;

                    // If this is the first level then check if we should filter out this data.
                    if (depth == 0) {
                        final CompiledFilter compiledFilter = compiledField.getCompiledFilter();
                        if (compiledFilter != null) {
                            generator = expression.createGenerator();
                            generator.set(values);

                            // If we are filtering then we need to evaluate this field
                            // now so that we can filter the resultant value.
                            value = generator.eval();

                            if (value != null && !compiledFilter.match(value.toString())) {
                                // We want to exclude this item so get out of this method ASAP.
                                return;
                            }
                        }
                    }

                    // If we are grouping at this level then evaluate the expression and add to the group values.
                    if (groupIndices[fieldIndex]) {
                        // If we haven't already created the generator then do so now.
                        if (generator == null) {
                            generator = expression.createGenerator();
                            generator.set(values);
                            value = generator.eval();
                        }
                        groupValues[groupIndex++] = value;
                    }

                    // If we need a value at this level then evaluate the expression and add the value.
                    if (valueIndices[fieldIndex]) {
                        // If we haven't already created the generator then do so now.
                        if (generator == null) {
                            generator = expression.createGenerator();
                            generator.set(values);
                        }
                        generators[fieldIndex] = generator;
                    }
                }
            }

            // Trim group values.
            if (groupIndex < groupSize) {
                groupValues = Arrays.copyOf(groupValues, groupIndex);
            }

            if (depth <= compiledDepths.getMaxGroupDepth()) {
                // This is a grouped item.
                final KeyPart keyPart = new GroupKeyPart(groupValues);
                key = key.resolve(keyPart, true);

            } else {
                // This item will not be grouped.
                final KeyPart keyPart = new UngroupedKeyPart(UUID.randomUUID().toString());
                key = key.resolve(keyPart, false);
            }

            put(new QueueItem(keySerde, valueSerde, key, generators));
        }
    }

    private void put(final QueueItem item) {
        LOGGER.trace(() -> "put");
        if (Thread.currentThread().isInterrupted() || hasEnoughData.get()) {
            return;
        }

        totalResultCount.incrementAndGet();

        // Some searches can be terminated early if the user is not sorting or grouping.
        if (!hasSort && !compiledDepths.hasGroup()) {
            // No sorting or grouping so we can stop the search as soon as we have the number of results requested by
            // the client
            if (maxResults != null && totalResultCount.get() >= maxResults.size(0)) {
                hasEnoughData.set(true);
            }
        }

        try {
            queue.put(Optional.of(item));
        } catch (final InterruptedException e) {
            LOGGER.debug(e.getMessage(), e);
            // Keep interrupting this thread.
            Thread.currentThread().interrupt();
        }
    }

    private void transfer() {
        Metrics.measure("Transfer", () -> {
            try {
                Txn<ByteBuffer> writeTxn = null;
                boolean run = true;
                boolean needsCommit = false;
                long lastCommitMs = System.currentTimeMillis();

                while (run) {
                    final Optional<QueueItem> optional = queue.poll(1, TimeUnit.SECONDS);
                    if (optional != null) {
                        if (optional.isPresent()) {
                            if (writeTxn == null) {
                                writeTxn = lmdbEnvironment.txnWrite();
                            }

                            final QueueItem item = optional.get();
                            insert(writeTxn, item);

                        } else {
                            // Stop looping.
                            run = false;
                            // Ensure final commit.
                            lastCommitMs = 0;
                        }

                        // We have either added something or need a final commit.
                        needsCommit = true;
                    }

                    if (createPayload.get() && currentPayload.get() == null) {
                        // Commit
                        if (writeTxn != null) {
                            // Commit
                            lastCommitMs = System.currentTimeMillis();
                            needsCommit = false;
                            commit(writeTxn);
                            writeTxn = null;
                        }

                        // Create payload and clear the DB.
                        currentPayload.set(createPayload());

                    } else if (needsCommit && writeTxn != null) {
                        final long now = System.currentTimeMillis();
                        if (lastCommitMs < now - COMMIT_FREQUENCY_MS) {
                            // Commit
                            lastCommitMs = now;
                            needsCommit = false;
                            commit(writeTxn);
                            writeTxn = null;
                        }
                    }
                }

            } catch (final InterruptedException e) {
                LOGGER.debug(e.getMessage(), e);
                // Continue to interrupt.
                Thread.currentThread().interrupt();
            } finally {
                addedData.countDown();

                // Drop the DB if we have been instructed to do so.
                if (drop.get()) {
                    drop();
                }
            }
        });
    }

    private void insert(final Txn<ByteBuffer> txn, final QueueItem queueItem) {
        Metrics.measure("Insert", () -> {
            try {
                LOGGER.trace(() -> "insert");

                final ByteBuffer keyBuffer = queueItem.getKeyBuffer();
                if (queueItem.isGrouped()) {
                    Metrics.measure("Grouped insert", () -> {
                        // See if we can find an existing item.
                        final ByteBuffer valueBuffer;
                        final ByteBuffer existing = Metrics.measure("Grouped get",
                                () -> lmdbDbi.get(txn, keyBuffer));
                        if (existing != null) {
                            final Generator[] existingValue = valueSerde.deserialize(existing);
                            final Generator[] newValue = queueItem.getValue();
                            final Generator[] combined = combine(existingValue, newValue);
                            valueBuffer = valueSerde.serialize(combined);

                        } else {
                            resultCount.incrementAndGet();
                            valueBuffer = queueItem.getValueBuffer();
                        }

                        Metrics.measure("Grouped put", () -> lmdbDbi.put(txn, keyBuffer, valueBuffer));
                    });

                } else {
                    Metrics.measure("Ungrouped insert", () -> {
                        resultCount.incrementAndGet();
                        final ByteBuffer valueBuffer = queueItem.getValueBuffer();
                        Metrics.measure("Ungrouped put", () -> lmdbDbi.put(txn, keyBuffer, valueBuffer));
                    });
                }
            } catch (final RuntimeException e) {
                LOGGER.error(e.getMessage(), e);
                throw new RuntimeException("Error putting " + queueItem, e);
            }
        });
    }

    private Generator[] combine(final Generator[] existing, final Generator[] value) {
        return Metrics.measure("Combine", () -> {
            // Combine the new item into the original item.
            for (int i = 0; i < existing.length; i++) {
                Generator existingGenerator = existing[i];
                Generator newGenerator = value[i];
                if (newGenerator != null) {
                    if (existingGenerator == null) {
                        existing[i] = newGenerator;
                    } else {
                        existingGenerator.merge(newGenerator);
                    }
                }
            }

            return existing;
        });
    }

    @Override
    public void clear() {
        // If the queue is still being transferred then set the drop flag and tell the transfer process to complete.
        drop.set(true);
        queue.clear();
        getCompletionState().complete();

        try {
            // If we are already complete then drop the DB directly.
            final boolean complete = addedData.await(0, TimeUnit.MILLISECONDS);
            if (complete) {
                drop();
            }
        } catch (final InterruptedException e) {
            LOGGER.debug(e.getMessage(), e);
            drop();
            Thread.currentThread().interrupt();
        }
    }

    private synchronized void drop() {
        if (!dropped.get()) {
            try (final Txn<ByteBuffer> writeTxn = lmdbEnvironment.txnWrite()) {
                LOGGER.info("Dropping: " + new String(lmdbDbi.getName(), StandardCharsets.UTF_8));
                lmdbDbi.drop(writeTxn, true);
                writeTxn.commit();
            } catch (final RuntimeException e) {
                LOGGER.error(e.getMessage(), e);
                lmdbEnvironment.list();
            } finally {
                resultCount.set(0);
                totalResultCount.set(0);
                dropped.set(true);
                lmdbEnvironment.list();
            }
        }
    }

    private byte[] createPayload() {
        final PayloadOutput payloadOutput = new PayloadOutput();

        Metrics.measure("createPayload", () -> {
            try (Txn<ByteBuffer> writeTxn = lmdbEnvironment.txnWrite()) {
                final long limit = lmdbConfig.getPayloadLimit().getBytes();
                if (limit > 0) {
                    final AtomicLong count = new AtomicLong();

                    try (final CursorIterable<ByteBuffer> cursorIterable = lmdbDbi.iterate(writeTxn)) {
                        final Iterator<KeyVal<ByteBuffer>> iterator = cursorIterable.iterator();
                        while (count.get() < limit && iterator.hasNext()) {
                            final KeyVal<ByteBuffer> kv = iterator.next();
                            final ByteBuffer keyBuffer = kv.key();
                            final ByteBuffer valBuffer = kv.val();

                            // Add to the size of the current payload.
                            count.addAndGet(keyBuffer.remaining());
                            count.addAndGet(valBuffer.remaining());

                            payloadOutput.writeInt(keyBuffer.remaining());
                            payloadOutput.writeByteBuffer(keyBuffer);
                            payloadOutput.writeInt(valBuffer.remaining());
                            payloadOutput.writeByteBuffer(valBuffer);

//                            final Key key = keySerde.deserialize(keyBuffer);
//                            final Generator[] value = valueSerde.deserialize(valBuffer);
//
//                            itemSerialiser.writeKey(key, output);
//                            itemSerialiser.writeGenerators(value, output);

                            lmdbDbi.delete(writeTxn, keyBuffer.flip());
                        }
                    }

                    writeTxn.commit();

                } else {
                    lmdbDbi.iterate(writeTxn).forEach(kv -> {
                        final ByteBuffer keyBuffer = kv.key();
                        final ByteBuffer valBuffer = kv.val();

                        payloadOutput.writeInt(keyBuffer.remaining());
                        payloadOutput.writeByteBuffer(keyBuffer);
                        payloadOutput.writeInt(valBuffer.remaining());
                        payloadOutput.writeByteBuffer(valBuffer);


//                        final Key key = keySerde.deserialize(keyBuffer);
//                        final Generator[] value = valueSerde.deserialize(valBuffer);
//
//                        itemSerialiser.writeKey(key, output);
//                        itemSerialiser.writeGenerators(value, output);
                    });

                    lmdbDbi.drop(writeTxn);
                    writeTxn.commit();
                }

            } catch (final RuntimeException e) {
                throw new RuntimeException("Error clearing DB", e);
            }
        });

        return payloadOutput.toBytes();
    }

    @Override
    public void writePayload(final Output output) {
        Metrics.measure("writePayload", () -> {
            try {
                final boolean complete = addedData.await(0, TimeUnit.MILLISECONDS);
                createPayload.set(true);

                final List<byte[]> payloads = new ArrayList<>(2);

                final byte[] payload = currentPayload.getAndSet(null);
                if (payload != null) {
                    payloads.add(payload);
                }

                if (complete) {
                    final byte[] finalPayload = createPayload();
                    payloads.add(finalPayload);
                }

                output.writeInt(payloads.size());
                payloads.forEach(bytes -> {
                    output.writeInt(bytes.length);
                    output.writeBytes(bytes);
                });

            } catch (final InterruptedException e) {
                LOGGER.debug(e.getMessage(), e);
                // Continue to interrupt this thread.
                Thread.currentThread().interrupt();
            }
        });
    }

    @Override
    public boolean readPayload(final Input input) {
        return Metrics.measure("readPayload", () -> {
            final int count = input.readInt(); // There may be more than one payload if it was the final transfer.
            for (int i = 0; i < count; i++) {
                final int length = input.readInt();
                if (length > 0) {
                    final byte[] bytes = input.readBytes(length);
                    try (final Input in = new Input(new ByteArrayInputStream(bytes))) {
                        while (!in.end()) {
                            final int keyLength = in.readInt();
                            final byte[] key = in.readBytes(keyLength);
                            final int valueLength = in.readInt();
                            final byte[] value = in.readBytes(valueLength);
                            final QueueItem queueItem = new QueueItem(
                                    keySerde,
                                    valueSerde,
                                    ByteBuffer.wrap(key),
                                    ByteBuffer.wrap(value));
                            put(queueItem);
                        }
                    }
                }
            }

            // Return success if we have not been asked to terminate and we are still willing to accept data.
            return !Thread.currentThread().isInterrupted() && !hasEnoughData.get();
        });
    }

    @Override
    public Items get() {
        return get(null);
    }

    @Override
    public Items get(final RawKey rawParentKey) {
        return Metrics.measure("get", () -> {
            Key parentKey = Key.root();
            if (rawParentKey != null) {
                parentKey = itemSerialiser.toKey(rawParentKey);
            }
            final int depth = parentKey.getDepth() + 1;
            final int trimmedSize = maxResults.size(depth);

            final ItemArrayList list = getChildren(parentKey, depth, trimmedSize, true, false);

            return new Items() {
                @Override
                @Nonnull
                public Iterator<Item> iterator() {
                    return new Iterator<>() {
                        private int pos = 0;

                        @Override
                        public boolean hasNext() {
                            return list.size > pos;
                        }

                        @Override
                        public Item next() {
                            return list.array[pos++];
                        }
                    };
                }

                @Override
                public int size() {
                    return list.size();
                }
            };
        });
    }

    private ItemArrayList getChildren(final Key parentKey,
                                      final int depth,
                                      final int trimmedSize,
                                      final boolean allowSort,
                                      final boolean trimTop) {
        final ItemArrayList list = new ItemArrayList(10);
        try (PooledByteBuffer pooledByteBuffer = byteBufferPool.getPooledByteBuffer(4096)) {
            final ByteBuffer byteBuffer = pooledByteBuffer.getByteBuffer();
            try (final Output output = new ByteBufferOutput(byteBuffer)) {
                itemSerialiser.writeChildKey(parentKey, output);
            }
            byteBuffer.flip();

            final KeyRange<ByteBuffer> keyRange = KeyRange.atLeast(byteBuffer);

            final int maxSize;
            if (trimmedSize < Integer.MAX_VALUE / 2) {
                maxSize = Math.max(1000, trimmedSize * 2);
            } else {
                maxSize = Integer.MAX_VALUE;
            }
            final CompiledSorter<HasGenerators> sorter = compiledSorters[depth];
            boolean trimmed = true;

            boolean inRange = true;
            try (final Txn<ByteBuffer> readTxn = lmdbEnvironment.txnRead()) {
                try (final CursorIterable<ByteBuffer> cursorIterable = lmdbDbi.iterate(readTxn, keyRange)) {
                    final Iterator<KeyVal<ByteBuffer>> iterator = cursorIterable.iterator();

                    while (iterator.hasNext() && inRange && !Thread.currentThread().isInterrupted()) {
                        final KeyVal<ByteBuffer> keyVal = iterator.next();
                        final Key key = keySerde.deserialize(keyVal.key());

                        if (!key.getParent().equals(parentKey)) {
                            inRange = false;

                        } else {
                            final byte[] keyBytes = ByteBufferUtils.toBytes(keyVal.key().flip());
                            final Generator[] generators = valueSerde.deserialize(keyVal.val());

                            list.add(new ItemImpl(this, new RawKey(keyBytes), key, generators));
                            if (!allowSort && list.size >= trimmedSize) {
                                // Stop without sorting etc.
                                inRange = false;

                            } else {
                                trimmed = false;
                                if (list.size() > maxSize) {
                                    list.sortAndTrim(sorter, trimmedSize, trimTop);
                                    trimmed = true;
                                }
                            }
                        }
                    }
                }
            }

            if (!trimmed) {
                list.sortAndTrim(sorter, trimmedSize, trimTop);
            }
        }

        return list;
    }


    @Override
    public long getSize() {
        return resultCount.get();
    }

    @Override
    public long getTotalSize() {
        return totalResultCount.get();
    }

    private static class ItemArrayList {

        private final int minArraySize;
        private ItemImpl[] array;
        private int size;

        public ItemArrayList(final int minArraySize) {
            this.minArraySize = minArraySize;
            array = new ItemImpl[minArraySize];
        }

        void sortAndTrim(final CompiledSorter<HasGenerators> sorter,
                         final int trimmedSize,
                         final boolean trimTop) {
            if (sorter != null && size > 0) {
                Arrays.sort(array, 0, size, sorter);
            }
            if (size > trimmedSize) {
                final int len = Math.max(minArraySize, trimmedSize);
                final ItemImpl[] newArray = new ItemImpl[len];
                if (trimTop) {
                    System.arraycopy(array, array.length - trimmedSize, newArray, 0, trimmedSize);
                } else {
                    System.arraycopy(array, 0, newArray, 0, trimmedSize);
                }
                array = newArray;
                size = trimmedSize;
            }
        }

        void add(final ItemImpl item) {
            if (array.length <= size) {
                final ItemImpl[] newArray = new ItemImpl[size * 2];
                System.arraycopy(array, 0, newArray, 0, array.length);
                array = newArray;
            }
            array[size++] = item;
        }

        ItemImpl get(final int index) {
            return array[index];
        }

        int size() {
            return size;
        }
    }

    public static class ItemImpl implements Item, HasGenerators {

        private final LmdbDataStore lmdbDataStore;
        private final RawKey rawKey;
        private final Key key;
        private final Generator[] generators;

        public ItemImpl(final LmdbDataStore lmdbDataStore,
                        final RawKey rawKey,
                        final Key key,
                        final Generator[] generators) {
            this.lmdbDataStore = lmdbDataStore;
            this.rawKey = rawKey;
            this.key = key;
            this.generators = generators;
        }

        @Override
        public RawKey getRawKey() {
            return rawKey;
        }

        @Override
        public Key getKey() {
            return key;
        }

        @Override
        public Val getValue(final int index) {
            Val val = null;

            final Generator generator = generators[index];
            if (generator instanceof Selector) {
                if (key.isGrouped()) {
                    int maxRows = 1;
                    boolean sort = true;
                    boolean trimTop = false;

                    if (generator instanceof AnySelector) {
                        sort = false;
//                    } else if (generator instanceof FirstSelector) {
                    } else if (generator instanceof LastSelector) {
                        trimTop = true;
                    } else if (generator instanceof TopSelector) {
                        maxRows = ((TopSelector) generator).getLimit();
                    } else if (generator instanceof BottomSelector) {
                        maxRows = ((BottomSelector) generator).getLimit();
                        trimTop = true;
                    } else if (generator instanceof NthSelector) {
                        maxRows = ((NthSelector) generator).getPos();
                    }

                    final ItemArrayList items = lmdbDataStore.getChildren(
                            key,
                            key.getDepth() + 1,
                            maxRows,
                            sort,
                            trimTop);

                    final Selector selector = (Selector) generator;
                    val = selector.select(new Selection<>() {
                        @Override
                        public int size() {
                            return items.size;
                        }

                        @Override
                        public Val get(final int pos) {
                            if (pos < items.size) {
                                items.get(pos).generators[index].eval();
                            }
                            return ValNull.INSTANCE;
                        }
                    });

                } else {
                    val = generator.eval();
                }
            } else if (generator != null) {
                val = generator.eval();
            }

            return val;
        }

        @Override
        public Generator[] getGenerators() {
            return generators;
        }
    }

    private static class QueueItem {

        private final KeySerde keySerde;
        private final ValueSerde valueSerde;
        private Key key;
        private Generator[] value;
        private ByteBuffer keyBuffer;
        private ByteBuffer valueBuffer;

        public QueueItem(final KeySerde keySerde,
                         final ValueSerde valueSerde,
                         final Key key,
                         final Generator[] value) {
            this.keySerde = keySerde;
            this.valueSerde = valueSerde;
            this.key = key;
            this.value = value;
        }

        public QueueItem(final KeySerde keySerde,
                         final ValueSerde valueSerde,
                         final ByteBuffer keyBuffer,
                         final ByteBuffer valueBuffer) {
            this.keySerde = keySerde;
            this.valueSerde = valueSerde;
            this.keyBuffer = keyBuffer;
            this.valueBuffer = valueBuffer;
        }

        public boolean isGrouped() {
            if (key != null) {
                return key.isGrouped();
            }
            return keyBuffer.get(keyBuffer.remaining() - 1) != 0;
        }

        public Key getKey() {
            if (key == null) {
                key = keySerde.deserialize(keyBuffer);
            }
            return key;
        }

        public Generator[] getValue() {
            if (value == null) {
                value = valueSerde.deserialize(valueBuffer);
            }
            return value;
        }

        public ByteBuffer getKeyBuffer() {
            if (keyBuffer == null) {
                keyBuffer = keySerde.serialize(key);
            }
            return keyBuffer;
        }

        public ByteBuffer getValueBuffer() {
            if (valueBuffer == null) {
                valueBuffer = valueSerde.serialize(value);
            }
            return valueBuffer;
        }

        @Override
        public String toString() {
            return "key=" +
                    getKey() +
                    ", value=" +
                    Arrays.toString(getValue());
        }
    }
}
