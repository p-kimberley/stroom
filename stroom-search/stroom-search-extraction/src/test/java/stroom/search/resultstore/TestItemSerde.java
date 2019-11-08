package stroom.search.resultstore;

import org.junit.Assert;
import org.junit.Test;
import stroom.dashboard.expression.v1.Expression;
import stroom.dashboard.expression.v1.ExpressionParser;
import stroom.dashboard.expression.v1.FieldIndexMap;
import stroom.dashboard.expression.v1.FunctionFactory;
import stroom.dashboard.expression.v1.Generator;
import stroom.dashboard.expression.v1.ParamFactory;
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
import stroom.query.common.v2.Item;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class TestItemSerde {
    @Test
    public void test() throws Exception {
        test("${val1}");
        test("min(${val1})");
        test("max(${val1})");
        test("sum(${val1})");
        test("min(round(${val1}, 4))");
        test("min(roundDay(${val1}))");
        test("min(roundMinute(${val1}))");
        test("ceiling(${val1})");
        test("floor(${val1})");
        test("ceiling(floor(min(roundMinute(${val1}))))");
        test("ceiling(floor(min(round(${val1}))))");
        test("max(${val1})-min(${val1})");
        test("max(${val1})/count()");
        test("round(${val1})/(min(${val1})+max(${val1}))");
        test("concat('this is', 'it')");
        test("concat('it''s a string', 'with a quote')");
        test("'it''s a string'");
        test("50");
        test("stringLength('it''s a string')");
        test("upperCase('it''s a string')");
        test("lowerCase('it''s a string')");
        test("encodeUrl('http://www.example.com')");
        test("decodeUrl('http://www.example.com')");
        test("substring('Hello', 0, 1)");
        test("equals(${val1}, ${val1})");
        test("greaterThan(1, 0)");
        test("lessThan(1, 0)");
        test("greaterThanOrEqualTo(1, 0)");
        test("lessThanOrEqualTo(1, 0)");
        test("1=0");
        test("decode('fred', 'fr.+', 'freda', 'freddy')");
        test("extractHostFromUri('http://www.example.com:1234/this/is/a/path')");
        test("link('title', 'http://www.somehost.com/somepath', 'target')");
        test("dashboard('title', 'someuuid', 'param1=value1')");
    }

    private void test(final String expressionString) throws Exception {
        final ByteBufferPool byteBufferPool = new ByteBufferPool();

        final FieldIndexMap fieldIndexMap = FieldIndexMap.forFields("val1");
        final ExpressionParser expressionParser = new ExpressionParser(new FunctionFactory(), new ParamFactory());
        final Expression expression = expressionParser.parse(fieldIndexMap, expressionString);
        for (int i = 0; i < 100; i++) {
            final Val[] values = new Val[] {ValString.create("test" + i)};
            final Generator generator = expression.createGenerator();
            generator.set(values);
            final GKey key = new GKey(ValString.create("test"));
            final Item item = new Item(key, new Generator[]{generator}, 0);

            byteBufferPool.context(Serde.DEFAULT_CAPACITY, byteBuffer -> {
                ItemSerde itemSerde = new ItemSerde();
                itemSerde.serialize(byteBuffer, item);

                print(byteBuffer);

                final Item deserialised = itemSerde.deserialize(byteBuffer);
                Assert.assertEquals(item, deserialised);
            });
        }
    }

    private void print(final ByteBuffer byteBuffer) {
        final ByteBuffer copy = byteBuffer.duplicate();
        byte[] bytes = new byte[copy.limit()];
        for (int i = 0; i < copy.limit(); i++) {
            bytes[i] = copy.get();
        }
        System.out.println(Arrays.toString(bytes));
    }
}
