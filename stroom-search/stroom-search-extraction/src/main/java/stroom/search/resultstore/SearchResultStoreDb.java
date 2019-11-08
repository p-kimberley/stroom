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

package stroom.search.resultstore;

import org.lmdbjava.Cursor;
import org.lmdbjava.CursorIterator;
import org.lmdbjava.CursorIterator.KeyVal;
import org.lmdbjava.Dbi;
import org.lmdbjava.DbiFlags;
import org.lmdbjava.Env;
import org.lmdbjava.PutFlags;
import org.lmdbjava.Txn;
import stroom.dashboard.expression.v1.Expression;
import stroom.dashboard.expression.v1.Generator;
import stroom.dashboard.expression.v1.GroupKey;
import stroom.lmdb.serde.Serde;
import stroom.lmdb.util.ByteBufferPool;
import stroom.mapreduce.v2.PairQueue;
import stroom.query.common.v2.Item;
import stroom.search.coprocessor.Combiner;
import stroom.util.logging.LambdaLogger;
import stroom.util.logging.LambdaLoggerFactory;

import javax.inject.Inject;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;

public class SearchResultStoreDb {
    private static final LambdaLogger LOGGER = LambdaLoggerFactory.getLogger(SearchResultStoreDb.class);

    public static final String DB_NAME = "SearchResultStore";

    public static final GroupKey UNGROUPED = new GroupKey(Collections.emptyList());

    private final Env<ByteBuffer> env;
    private final ByteBufferPool byteBufferPool;
    private final GroupKeySerde keySerde;
    private final GeneratorsSerde valueSerde;
    private final Dbi<ByteBuffer> lmdbDbi;

    @Inject
    SearchResultStoreDb(final Env<ByteBuffer> env,
                        final ByteBufferPool byteBufferPool,
                        final GroupKeySerde keySerde,
                        final GeneratorsSerde valueSerde) {
        this.env = env;
        this.byteBufferPool = byteBufferPool;
        this.keySerde = keySerde;
        this.valueSerde = valueSerde;
        this.lmdbDbi = openDbi(env, DB_NAME);
    }

    public boolean combine(final GroupKey key, final Item value, final Combiner combiner, final Expression[] expressions) {
        final AtomicBoolean success = new AtomicBoolean();

        byteBufferPool.context(Serde.DEFAULT_CAPACITY, keyBuffer -> {
            keySerde.serialize(keyBuffer, key);

            byteBufferPool.context(Serde.DEFAULT_CAPACITY, valueBuffer -> {
                valueSerde.serialize(valueBuffer, value);

                try (final Txn<ByteBuffer> txn = env.txnWrite()) {
                    if (key != null && !UNGROUPED.equals(key)) {
                        byteBufferPool.context(Serde.DEFAULT_CAPACITY, currentValueBuffer -> {
                            ByteBuffer existingValueBuffer = lmdbDbi.get(txn, keyBuffer);
                            if (existingValueBuffer == null) {
                                success.set(lmdbDbi.put(txn, keyBuffer, valueBuffer));
                            } else {


                                final Generators generators = new Generators(value.getGenerators());
                                final Item currentValue = valueSerde.deserialize(existingValueBuffer);
                                final Item newValue = combiner.combine(currentValue, value);
                                byteBufferPool.context(Serde.DEFAULT_CAPACITY, newValueBuffer -> {
                                    valueSerde.serialize(newValueBuffer, newValue);
//                                    lmdbDbi.delete(txn, keyBuffer);
                                    success.set(lmdbDbi.put(txn, keyBuffer, newValueBuffer));
                                });
                            }
                        });

                    } else {
                        // You can only append multiple values to LMDB via a cursor for some reason.
                        try (final Cursor<ByteBuffer> cursor = lmdbDbi.openCursor(txn)) {
                            success.set(cursor.put(keyBuffer, valueBuffer));
                        }
                    }

                    txn.commit();
                } catch (RuntimeException e) {
                    throw new RuntimeException(LambdaLogger.buildMessage("Error putting key {}, value {}", key, value), e);
                }
            });
        });

        return success.get();
    }

    public void transfer(final PairQueue<GroupKey, Item> outputQueue) {
        try (final Txn<ByteBuffer> txn = env.txnRead()) {
            CursorIterator<ByteBuffer> cursorIterator = lmdbDbi.iterate(txn);
            while (cursorIterator.hasNext()) {
                KeyVal<ByteBuffer> keyVal = cursorIterator.next();
                final GroupKey key = keySerde.deserialize(keyVal.key());
                final Item item = valueSerde.deserialize(keyVal.val());
                if (UNGROUPED.equals(key)) {
                    outputQueue.collect(null, item);
                } else {
                    outputQueue.collect(key, item);
                }
            }
        }
    }

    private static Dbi<ByteBuffer> openDbi(final Env<ByteBuffer> env, final String name) {
        LOGGER.debug(() -> "Opening LMDB database with name: " + name);
        try {
            return env.openDbi(name, DbiFlags.MDB_CREATE, DbiFlags.MDB_DUPSORT);
        } catch (Exception e) {
            throw new RuntimeException(LambdaLogger.buildMessage("Error opening LMDB daatabase {}", name), e);
        }
    }
}
