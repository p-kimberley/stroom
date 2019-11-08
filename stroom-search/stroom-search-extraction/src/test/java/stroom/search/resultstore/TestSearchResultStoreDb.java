package stroom.search.resultstore;

import org.junit.Test;
import stroom.dashboard.expression.v1.Expression;
import stroom.dashboard.expression.v1.ExpressionParser;
import stroom.dashboard.expression.v1.FieldIndexMap;
import stroom.dashboard.expression.v1.FunctionFactory;
import stroom.dashboard.expression.v1.Generator;
import stroom.dashboard.expression.v1.ParamFactory;
import stroom.dashboard.expression.v1.StaticValueFunction;
import stroom.dashboard.expression.v1.Val;
import stroom.dashboard.expression.v1.ValString;
import stroom.mapreduce.v2.Pair;
import stroom.mapreduce.v2.UnsafePairQueue;
import stroom.query.common.v2.GroupKey;
import stroom.query.common.v2.Item;
import stroom.search.coprocessor.Combiner;

public class TestSearchResultStoreDb {
    @Test
    public void test() {
        final LmdbEnvironmentFactory lmdbEnvironmentFactory = new LmdbEnvironmentFactory();
        final LmdbEnvironment lmdbEnvironment = lmdbEnvironmentFactory.create();
        final SearchResultStoreDbFactory searchResultStoreDbFactory = new SearchResultStoreDbFactory();
        final SearchResultStoreDb searchResultStoreDb = searchResultStoreDbFactory.create(lmdbEnvironment);
        final Combiner combiner = new Combiner(new int[]{-1}, 1);

        for (int i = 0; i < 100; i++) {
            final StaticValueFunction staticValueFunction = new StaticValueFunction(ValString.create("test" + i));
            final Generator generator = staticValueFunction.createGenerator();
            final Item item = new Item(null, new Generator[]{generator}, 0);
            searchResultStoreDb.combine(GKey.UNGROUPED, item, combiner);
        }

        final UnsafePairQueue<GroupKey, Item> queue = new UnsafePairQueue<>();
        searchResultStoreDb.transfer(queue);

        for (final Pair<GroupKey, Item> pair : queue) {
            System.out.println(pair.toString());
        }
    }

    @Test
    public void testGrouped() throws Exception {
        final LmdbEnvironmentFactory lmdbEnvironmentFactory = new LmdbEnvironmentFactory();
        final LmdbEnvironment lmdbEnvironment = lmdbEnvironmentFactory.create();
        final SearchResultStoreDbFactory searchResultStoreDbFactory = new SearchResultStoreDbFactory();
        final SearchResultStoreDb searchResultStoreDb = searchResultStoreDbFactory.create(lmdbEnvironment);
        final Combiner combiner = new Combiner(new int[]{-1}, 1);


        final FieldIndexMap fieldIndexMap = FieldIndexMap.forFields("value1");
        final ExpressionParser expressionParser = new ExpressionParser(new FunctionFactory(), new ParamFactory());
        final Expression expression = expressionParser.parse(fieldIndexMap, "count()");
        for (int i = 0; i < 100; i++) {
            final Val[] values = new Val[] {ValString.create("test" + i)};
            final Generator generator = expression.createGenerator();
            generator.set(values);
//            final StaticValueFunction staticValueFunction = new StaticValueFunction(ValString.create("test" + i));
//            final Generator generator = staticValueFunction.createGenerator();
            final GKey key = new GKey(ValString.create("test"));
            final Item item = new Item(key, new Generator[]{generator}, 0);
            searchResultStoreDb.combine(key, item, combiner);
        }

        final UnsafePairQueue<GroupKey, Item> queue = new UnsafePairQueue<>();
        searchResultStoreDb.transfer(queue);

        for (final Pair<GroupKey, Item> pair : queue) {
            System.out.println(pair.toString());
        }
    }
}
