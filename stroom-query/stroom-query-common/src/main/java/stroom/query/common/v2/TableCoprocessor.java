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

import stroom.dashboard.expression.v1.Val;
import stroom.lmdb.serdes.LongSerde;

import java.util.concurrent.atomic.AtomicLong;

public class TableCoprocessor implements Coprocessor {
    private static final int DEFAULT_QUEUE_CAPACITY = 1000000;

    private final String coprocessorId;
//    private final LinkedBlockingQueue<byte[]> queue;
//    private final ItemMapper mapper;
//    private final Kryo kryo;
    private final AtomicLong writeSequence = new AtomicLong();
    private volatile long lastRead;
    private volatile long lastWrite;

    private final RawResultStoreDb rawResultStoreDb;

    public TableCoprocessor(final TableCoprocessorSettings settings,
                            final RawResultStoreDbFactory rawResultStoreDbFactory) {
        // This isn't very nice but is currently required to populate the required field names.
//        final TableSettings tableSettings = settings.getTableSettings();
//
//        final List<Field> fields = tableSettings.getFields();
////        final CompiledDepths compiledDepths = new CompiledDepths(fields, tableSettings.showDetail());
//        final CompiledFields compiledFields = new CompiledFields(fields, fieldNames, paramMap);

        coprocessorId = settings.getCoprocessorId();
//        queue = new LinkedBlockingQueue<>(DEFAULT_QUEUE_CAPACITY);
//        mapper = new ItemMapper(compiledFields, compiledDepths.getMaxDepth(), compiledDepths.getMaxGroupDepth());
//
//        kryo = new KryoFactory().get();

        final LongSerde keySerde = new LongSerde();
        final ValArraySerde valueSerde = ValArraySerde.create(settings.getFields());
        final String dbName = settings.getQueryKey().toString() + "_" + coprocessorId + "_" + "raw";
        rawResultStoreDb = rawResultStoreDbFactory.create(keySerde, valueSerde, dbName);


//try {
//    // We need a storage directory first.
//    // The path cannot be on a remote file system.
//    final Path path = tempDirProvider.get().resolve("search").resolve(key);
//    Files.createDirectories(path);
//
//    // We always need an Env. An Env owns a physical on-disk storage file. One
//    // Env can store many different databases (ie sorted maps).
//    final Env<ByteBuffer> env = Env.create()
//            // LMDB also needs to know how large our DB might be. Over-estimating is OK.
//            .setMapSize(ModelStringUtil.parseIECByteSizeString("10G"))
//            // LMDB also needs to know how many DBs (Dbi) we want to store in this Env.
//            .setMaxDbs(1)
//            // Now let's open the Env. The same path can be concurrently opened and
//            // used in different processes, but do not open the same path twice in
//            // the same process at the same time.
//            .open(path.toFile());
//
//    // We need a Dbi for each DB. A Dbi roughly equates to a sorted map. The
//    // MDB_CREATE flag causes the DB to be created if it doesn't already exist.
//    final Dbi<ByteBuffer> db = env.openDbi("raw", DbiFlags.MDB_CREATE);
//
//    // We want to store some data, so we will need a direct ByteBuffer.
//    // Note that LMDB keys cannot exceed maxKeySize bytes (511 bytes by default).
//    // Values can be larger.
//    final ByteBuffer key = ByteBuffer.allocateDirect(env.getMaxKeySize());
//    final ByteBuffer val = ByteBuffer.allocateDirect(700);
//    key.put("greeting".getBytes(StandardCharsets.UTF_8)).flip();
//    val.put("Hello world".getBytes(StandardCharsets.UTF_8)).flip();
//    final int valSize = val.remaining();
//
//    // Now store it. Dbi.put() internally begins and commits a transaction (Txn).
//    db.put(key, val);
//} catch (final IOException e) {
//
//}
    }

    @Override
    public void receive(final Val[] values) {
        final long key = writeSequence.incrementAndGet();
        rawResultStoreDb.put(key, values, false);
        lastWrite = key;
//        System.out.println(Arrays.toString(values));
//
//
//        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
//        try (final Output output = new Output(outputStream)) {
//            mapper.toKryo(values, kryo, output);
//            output.flush();
//        }
//        final byte[] bytes = outputStream.toByteArray();
//        try {
//            queue.put(bytes);
//        } catch (final InterruptedException e) {
//            Thread.currentThread().interrupt();
//        }





//        print(buffer);
//
//        try (final Input input = new Input(new ByteBufferInputStream(buffer))) {
//            outputGenerator.read(kryo, input);
//        }
//
//        final Val newVal = outputGenerator.eval();
//
//        assertThat(newVal).isEqualTo(val);
//
//
//
//
//


//        mapper.map(values, item -> {
//            try {
//                queue.put(item);
//            } catch (final InterruptedException e) {
//                Thread.currentThread().interrupt();
//            }
//        });
    }

    @Override
    public Payload createPayload() {
        final long from = lastRead + 1;
        final long to = lastWrite;
        final byte[] bytes = rawResultStoreDb.getRange(from, to);
        lastRead = to;





//        final List<byte[]> outputQueue = new ArrayList<>();
//        queue.drainTo(outputQueue);
        // Don't create a payload if the queue is empty.
        if (bytes.length == 0) {
            return null;
        } else {
            return new TablePayload(coprocessorId, 1, bytes);
        }

//            if (outputQueue.size() == 1) {
//                return new TablePayload(key, 1, outputQueue.get(0));
//            } else {
//                try {
//                    final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
//                    for (final byte[] data : outputQueue) {
//                        outputStream.write(data);
//                    }
//                    final byte[] data = outputStream.toByteArray();
//                    return new TablePayload(key, outputQueue.size(), data);
//                } catch (final IOException e) {
//                    throw new RuntimeException(e.getMessage(), e);
//                }
//            }
//        }
    }
}
