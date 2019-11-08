package stroom.search.resultstore;

import org.junit.Assert;
import org.junit.Test;
import stroom.dashboard.expression.v1.Val;
import stroom.dashboard.expression.v1.ValBoolean;
import stroom.dashboard.expression.v1.ValDouble;
import stroom.dashboard.expression.v1.ValErr;
import stroom.dashboard.expression.v1.ValInteger;
import stroom.dashboard.expression.v1.ValLong;
import stroom.dashboard.expression.v1.ValNull;
import stroom.dashboard.expression.v1.ValString;
import stroom.lmdb.serde.Serde;
import stroom.lmdb.util.ByteBufferPool;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class TestKeySerde {
    @Test
    public void test() {
        test(new GKey());
        test(new GKey(ValBoolean.create(true)));
        test(new GKey(ValDouble.create(1234D)));
        test(new GKey(ValErr.INSTANCE));
        test(new GKey(ValErr.create("test")));
        test(new GKey(ValInteger.create(1234)));
        test(new GKey(ValLong.create(1234L)));
        test(new GKey(ValNull.INSTANCE));
        test(new GKey(ValString.create("test")));
        test(new GKey(createList(ValString.create("test"), ValLong.create(1002L))));

        final GKey parent = new GKey(createList(ValString.create("test"), ValLong.create(1002L)));
        test(new GKey(parent, createList(ValDouble.create(12345D), ValInteger.create(12345), ValBoolean.create(true))));
        test(new GKey(parent, ValNull.INSTANCE));
        test(new GKey(parent, ValErr.INSTANCE));
        test(new GKey(parent, ValErr.create("test")));
    }

    private void test(final GKey key) {
        final ByteBufferPool byteBufferPool = new ByteBufferPool();
        byteBufferPool.context(Serde.DEFAULT_CAPACITY, byteBuffer -> {
            GKeySerde keySerde = new GKeySerde();
            keySerde.serialize(byteBuffer, key);

            print(byteBuffer);

            final GKey deserialised = keySerde.deserialize(byteBuffer);
            Assert.assertEquals(key, deserialised);
        });
    }

    private void print(final ByteBuffer byteBuffer) {
        final ByteBuffer copy = byteBuffer.duplicate();
        byte[] bytes = new byte[copy.limit()];
        for (int i = 0; i < copy.limit(); i++) {
            bytes[i] = copy.get();
        }
        System.out.println(Arrays.toString(bytes));
    }

    private List<Val> createList(Val... vals) {
        if (vals.length == 0) {
            return Collections.emptyList();
        }
        if (vals.length == 1) {
            return Collections.singletonList(vals[0]);
        }
        return new ArrayList<>(Arrays.asList(vals));
    }
}
