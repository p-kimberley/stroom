package stroom.query.common.v2;

import stroom.dashboard.expression.v1.Val;
import stroom.dashboard.expression.v1.ValString;
import stroom.datasource.api.v2.AbstractField;
import stroom.datasource.api.v2.TextField;
import stroom.lmdb.LmdbConfig;
import stroom.lmdb.LmdbEnvFactory;
import stroom.lmdb.bytebuffer.ByteBufferPool;
import stroom.lmdb.bytebuffer.ByteBufferPoolConfig;
import stroom.lmdb.bytebuffer.ByteBufferPoolFactory;
import stroom.query.api.v2.Field;
import stroom.query.api.v2.Format.Type;
import stroom.query.api.v2.QueryKey;
import stroom.query.api.v2.TableSettings;
import stroom.util.io.PathCreator;
import stroom.util.io.TempDirProvider;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.lmdbjava.Env;

import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class TestTableCoprocessor {
    @Test
    void testTableCoprocessor(@TempDir Path tempDir) {
        final Field text = new Field.Builder()
                .name("Text")
                .expression("${Text}")
                .format(Type.TEXT)
                .group(0)
                .build();
        final Field count = new Field.Builder()
                .name("Count")
                .expression("count()")
                .format(Type.NUMBER)
                .build();
        final TableSettings tableSettings = new TableSettings.Builder()
                .addFields(text, count)
                .extractValues(true)
                .extractionPipeline(
                        "Pipeline",
                        "dec68bf2-d2b5-440e-ab88-4d48aed5f12c",
                        "Search result text")
                .build();
        final String[] fieldNames = new String[]{"Text"};
        final AbstractField[] fields = new AbstractField[]{new TextField("Text")};
        final TableCoprocessorSettings tableCoprocessorSettings =
                new TableCoprocessorSettings(
                        new QueryKey(UUID.randomUUID().toString()),
                        "1",
                        Collections.singletonList("table-1"),
                        tableSettings,
                        fieldNames,
                        fields);

        final TempDirProvider tempDirProvider = () -> tempDir;
        final LmdbEnvFactory lmdbEnvFactory = new LmdbEnvFactory(tempDirProvider, new PathCreator(tempDirProvider));
        final LmdbConfig lmdbConfig = new LmdbConfig();
        final Env<ByteBuffer> lmdbEnvironment = lmdbEnvFactory.create(lmdbConfig);
        final ByteBufferPool byteBufferPool = new ByteBufferPoolFactory(new ByteBufferPoolConfig()).getByteBufferPool();
        final RawResultStoreDbFactory rawResultStoreDbFactory = (keySerde, valueSerde, dbName) -> new RawResultStoreDb(
                lmdbEnvironment,
                byteBufferPool,
                keySerde,
                valueSerde,
                dbName);
        final TableCoprocessor tableCoprocessor = new TableCoprocessor(
                tableCoprocessorSettings,
                rawResultStoreDbFactory);

        final Val[][] values = gen();
        for (final Val[] value : values) {
            tableCoprocessor.receive(value);
        }
        final Payload payload = tableCoprocessor.createPayload();
        final TablePayload tablePayload = (TablePayload) payload;

        final Sizes sizes = Sizes.create(List.of(1000000, 100, 10, 1), 1);
        final ValArraySerde valArraySerde = ValArraySerde.create(fields);
        final TableDataFactory tableDataFactory =
                new TableDataFactory(tableSettings, Collections.emptyMap(), sizes, sizes);
        final TablePayloadHandler tablePayloadHandler = new TablePayloadHandler(valArraySerde, tableDataFactory);


        tablePayloadHandler.process(tablePayload);
        final Data data = tableDataFactory.getData();


        System.out.println(payload);
        System.out.println(data);


    }


    private Val[][] gen() {
        final String[] arr = {
                "0001",
                "0001",
                "Execute",
                "Execute",
                "Application",
                "Application",
                "msg=foo bar",
                "Another message that I made up 2",
                "Rule",
                "Rule",
                "false",
                "false",
                "E0567",
                "E0567",
                "0001",
                "Execute",
                "Application",
                "msg=foo bar",
                "Rule",
                "false",
                "E0567",
                "0001",
                "Execute",
                "Application",
                "msg=foo bar",
                "Rule",
                "false",
                "E0567",
                "0001",
                "Execute",
                "Application",
                "msg=foo bar",
                "Rule",
                "false",
                "E0567",
        };
        final Val[][] vals = new Val[arr.length][];
        for (int i = 0; i < arr.length; i++) {
            vals[i] = new Val[]{ValString.create(arr[i])};
        }
        return vals;
    }
}
