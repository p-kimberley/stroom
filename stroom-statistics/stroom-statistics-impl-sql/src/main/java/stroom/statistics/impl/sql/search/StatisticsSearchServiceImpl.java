package stroom.statistics.impl.sql.search;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import io.reactivex.Flowable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import stroom.dashboard.expression.v1.FieldIndexMap;
import stroom.dashboard.expression.v1.Val;
import stroom.dashboard.expression.v1.ValDouble;
import stroom.dashboard.expression.v1.ValLong;
import stroom.dashboard.expression.v1.ValNull;
import stroom.dashboard.expression.v1.ValString;
import stroom.statistics.impl.sql.PreparedStatementUtil;
import stroom.statistics.impl.sql.SQLStatisticConstants;
import stroom.statistics.impl.sql.SQLStatisticNames;
import stroom.statistics.impl.sql.SQLStatisticsDbConnProvider;
import stroom.statistics.impl.sql.SqlBuilder;
import stroom.statistics.impl.sql.rollup.RollUpBitMask;
import stroom.statistics.impl.sql.shared.StatisticStoreDoc;
import stroom.statistics.impl.sql.shared.StatisticType;
import stroom.task.api.TaskContext;
import stroom.util.logging.LambdaLogger;
import stroom.util.logging.LambdaLoggerFactory;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@SuppressWarnings("unused")
        //called by DI
//TODO rename to StatisticsDatabaseSearchServiceImpl
class StatisticsSearchServiceImpl implements StatisticsSearchService {
    private static final Logger LOGGER = LoggerFactory.getLogger(StatisticsSearchServiceImpl.class);
    private static final LambdaLogger LAMBDA_LOGGER = LambdaLoggerFactory.getLogger(StatisticsSearchServiceImpl.class);

    private static final String KEY_TABLE_ALIAS = "K";
    private static final String VALUE_TABLE_ALIAS = "V";
    private static final String ALIASED_TIME_MS_COL = VALUE_TABLE_ALIAS + "." + SQLStatisticNames.TIME_MS;
    private static final String ALIASED_PRECISION_COL = VALUE_TABLE_ALIAS + "." + SQLStatisticNames.PRECISION;
    private static final String ALIASED_COUNT_COL = VALUE_TABLE_ALIAS + "." + SQLStatisticNames.COUNT;
    private static final String ALIASED_VALUE_COL = VALUE_TABLE_ALIAS + "." + SQLStatisticNames.VALUE;

    private final SQLStatisticsDbConnProvider SQLStatisticsDbConnProvider;
    private final SearchConfig searchConfig;
    private final Provider<TaskContext> taskContextProvider;

    //defines how the entity fields relate to the table columns
    private static final Map<String, List<String>> STATIC_FIELDS_TO_COLUMNS_MAP = ImmutableMap.<String, List<String>>builder()
            .put(StatisticStoreDoc.FIELD_NAME_DATE_TIME, Collections.singletonList(ALIASED_TIME_MS_COL))
            .put(StatisticStoreDoc.FIELD_NAME_PRECISION_MS, Collections.singletonList(ALIASED_PRECISION_COL))
            .put(StatisticStoreDoc.FIELD_NAME_COUNT, Collections.singletonList(ALIASED_COUNT_COL))
            .put(StatisticStoreDoc.FIELD_NAME_VALUE, Arrays.asList(ALIASED_COUNT_COL, ALIASED_VALUE_COL))
            .build();

    @SuppressWarnings("unused") // Called by DI
    @Inject
    StatisticsSearchServiceImpl(final SQLStatisticsDbConnProvider SQLStatisticsDbConnProvider,
                                final SearchConfig searchConfig,
                                final Provider<TaskContext> taskContextProvider) {
        this.SQLStatisticsDbConnProvider = SQLStatisticsDbConnProvider;
        this.searchConfig = searchConfig;
        this.taskContextProvider = taskContextProvider;
    }

    @Override
    public Flowable<Val[]> search(final StatisticStoreDoc statisticStoreEntity,
                                  final FindEventCriteria criteria,
                                  final FieldIndexMap fieldIndexMap) {

        List<String> selectCols = getSelectColumns(statisticStoreEntity, fieldIndexMap);
        SqlBuilder sql = buildSql(statisticStoreEntity, criteria, fieldIndexMap);

        // build a mapper function to convert a resultSet row into a String[] based on the fields
        // required by all coprocessors
        Function<ResultSet, Val[]> resultSetMapper = buildResultSetMapper(fieldIndexMap, statisticStoreEntity);

        // the query will not be executed until somebody subscribes to the flowable
        return getFlowableQueryResults(sql, resultSetMapper);
    }

    private List<String> getSelectColumns(final StatisticStoreDoc statisticStoreEntity,
                                          final FieldIndexMap fieldIndexMap) {
        //assemble a map of how fields map to 1-* select cols

        //get all the static field mappings
        final Map<String, List<String>> fieldToColumnsMap = new HashMap<>(STATIC_FIELDS_TO_COLUMNS_MAP);

        //now add in all the dynamic tag field mappings
        statisticStoreEntity.getFieldNames().forEach(tagField ->
                fieldToColumnsMap.computeIfAbsent(tagField, k -> new ArrayList<>())
                        .add(KEY_TABLE_ALIAS + "." + SQLStatisticNames.NAME));

        //now map the fields in use to a distinct list of columns
        return fieldToColumnsMap.entrySet().stream()
                .flatMap(entry ->
                        entry.getValue().stream()
                                .map(colName ->
                                        getOptFieldIndexPosition(fieldIndexMap, entry.getKey())
                                                .map(val -> colName))
                                .filter(Optional::isPresent)
                                .map(Optional::get))
                .distinct()
                .collect(Collectors.toList());
    }

    /**
     * Construct the sql select for the query's criteria
     */
    private SqlBuilder buildSql(final StatisticStoreDoc statisticStoreEntity,
                                final FindEventCriteria criteria,
                                final FieldIndexMap fieldIndexMap) {
        /**
         * SQL for testing querying the stat/tag names
         * <p>
         * create table test (name varchar(255)) ENGINE=InnoDB DEFAULT
         * CHARSET=latin1;
         * <p>
         * insert into test values ('StatName1');
         * <p>
         * insert into test values ('StatName2¬Tag1¬Val1¬Tag2¬Val2');
         * <p>
         * insert into test values ('StatName2¬Tag2¬Val2¬Tag1¬Val1');
         * <p>
         * select * from test where name REGEXP '^StatName1(¬|$)';
         * <p>
         * select * from test where name REGEXP '¬Tag1¬Val1(¬|$)';
         */

        final RollUpBitMask rollUpBitMask = buildRollUpBitMaskFromCriteria(criteria, statisticStoreEntity);

        final String statNameWithMask = statisticStoreEntity.getName() + rollUpBitMask.asHexString();

        SqlBuilder sql = new SqlBuilder();
        sql.append("SELECT ");

        String selectColsStr = getSelectColumns(statisticStoreEntity, fieldIndexMap).stream()
                .collect(Collectors.joining(", "));

        sql.append(selectColsStr);

        // join to key table
        sql.append(" FROM " + SQLStatisticNames.SQL_STATISTIC_KEY_TABLE_NAME + " K");
        sql.join(SQLStatisticNames.SQL_STATISTIC_VALUE_TABLE_NAME,
                "V",
                "K",
                SQLStatisticNames.ID,
                "V",
                SQLStatisticNames.SQL_STATISTIC_KEY_FOREIGN_KEY);

        // do a like on the name first so we can hit the index before doing the slow regex matches
        sql.append(" WHERE K." + SQLStatisticNames.NAME + " LIKE ");
        sql.arg(statNameWithMask + "%");

        // exact match on the stat name bit of the key
        sql.append(" AND K." + SQLStatisticNames.NAME + " REGEXP ");
        sql.arg("^" + statNameWithMask + "(" + SQLStatisticConstants.NAME_SEPARATOR + "|$)");

        // add the time bounds
        sql.append(" AND V." + SQLStatisticNames.TIME_MS + " >= ");
        sql.arg(criteria.getPeriod().getFromMs());
        sql.append(" AND V." + SQLStatisticNames.TIME_MS + " < ");
        sql.arg(criteria.getPeriod().getToMs());

        // now add the query terms
        SQLTagValueWhereClauseConverter.buildTagValueWhereClause(criteria.getFilterTermsTree(), sql);

        final int maxResults = searchConfig.getMaxResults();
        sql.append(" LIMIT " + maxResults);

        LOGGER.debug("Search query: {}", sql.toString());

        return sql;
    }

    private Optional<Integer> getOptFieldIndexPosition(final FieldIndexMap fieldIndexMap, final String fieldName) {
        int idx = fieldIndexMap.get(fieldName);
        if (idx == -1) {
            return Optional.empty();
        } else {
            return Optional.of(idx);
        }
    }


    /**
     * Build a mapper function that will only extract the columns of interest from the resultSet row.
     * Assumes something external to the returned function will advance the resultSet
     */
    private Function<ResultSet, Val[]> buildResultSetMapper(
            final FieldIndexMap fieldIndexMap,
            final StatisticStoreDoc statisticStoreEntity) {

        LAMBDA_LOGGER.debug(() -> String.format("Building mapper for fieldIndexMap %s, entity %s",
                fieldIndexMap, statisticStoreEntity.getUuid()));

        // construct a list of field extractors that can populate the appropriate bit of the data arr
        // when given a resultSet row
        List<ValueExtractor> valueExtractors = fieldIndexMap.getMap().entrySet().stream()
                .map(entry -> {
                    final int idx = entry.getValue();
                    final String fieldName = entry.getKey();
                    final ValueExtractor extractor;
                    if (fieldName.equals(StatisticStoreDoc.FIELD_NAME_DATE_TIME)) {
                        extractor = buildLongValueExtractor(SQLStatisticNames.TIME_MS, idx);
                    } else if (fieldName.equals(StatisticStoreDoc.FIELD_NAME_COUNT)) {
                        extractor = buildLongValueExtractor(SQLStatisticNames.COUNT, idx);
                    } else if (fieldName.equals(StatisticStoreDoc.FIELD_NAME_PRECISION_MS)) {
                        extractor = buildPrecisionMsExtractor(idx);
                    } else if (fieldName.equals(StatisticStoreDoc.FIELD_NAME_VALUE)) {
                        final StatisticType statisticType = statisticStoreEntity.getStatisticType();
                        if (statisticType.equals(StatisticType.COUNT)) {
                            extractor = buildLongValueExtractor(SQLStatisticNames.COUNT, idx);
                        } else if (statisticType.equals(StatisticType.VALUE)) {
                            // value stat
                            extractor = buildStatValueExtractor(idx);
                        } else {
                            throw new RuntimeException(String.format("Unexpected type %s", statisticType));
                        }
                    } else if (statisticStoreEntity.getFieldNames().contains(fieldName)) {
                        // this is a tag field so need to extract the tags/values from the NAME col.
                        // We only want to do this extraction once so we cache the values
                        extractor = buildTagFieldValueExtractor(fieldName, idx);
                    } else {
                        throw new RuntimeException(String.format("Unexpected fieldName %s", fieldName));
                    }
                    LAMBDA_LOGGER.debug(() ->
                            String.format("Adding extraction function for field %s, idx %s", fieldName, idx));
                    return extractor;
                })
                .collect(Collectors.toList());

        final int arrSize = valueExtractors.size();

        //the mapping function that will be used on each row in the resultSet, that makes use of the ValueExtractors
        //created above
        return rs -> {
            Preconditions.checkNotNull(rs);
            try {
                if (rs.isClosed()) {
                    throw new RuntimeException("ResultSet is closed");
                }
            } catch (SQLException e) {
                throw new RuntimeException("Error testing closed state of resultSet", e);
            }
            //the data array we are populating
            final Val[] data = new Val[arrSize];
            //state to hold while mapping this row, used to save parsing the NAME col multiple times
            final Map<String, Val> fieldValueCache = new HashMap<>();

            //run each of our field value extractors against the resultSet to fill up the data arr
            valueExtractors.forEach(valueExtractor ->
                    valueExtractor.extract(rs, data, fieldValueCache));

            LAMBDA_LOGGER.trace(() -> {
                try {
                    return String.format("Mapped resultSet row %s to %s", rs.getRow(), Arrays.toString(data));
                } catch (SQLException e) {
                    throw new RuntimeException(String.format("Error getting current row number: %s", e.getMessage()), e);
                }
            });
            return data;
        };
    }

    private ValueExtractor buildPrecisionMsExtractor(final int idx) {
        final ValueExtractor extractor;
        extractor = (rs, arr, cache) -> {
            // the precision in the table represents the number of zeros
            // of millisecond precision, e.g.
            // 6=1,000,000ms
            final long precisionMs;
            try {
                precisionMs = (long) Math.pow(10, rs.getInt(SQLStatisticNames.PRECISION));
            } catch (SQLException e) {
                throw new RuntimeException("Error extracting precision field", e);
            }
            arr[idx] = ValLong.create(precisionMs);
        };
        return extractor;
    }

    private ValueExtractor buildStatValueExtractor(final int idx) {
        final ValueExtractor extractor;
        extractor = (rs, arr, cache) -> {

            final double aggregatedValue;
            final long count;
            try {
                aggregatedValue = rs.getDouble(SQLStatisticNames.VALUE);
                count = rs.getLong(SQLStatisticNames.COUNT);
            } catch (SQLException e) {
                throw new RuntimeException("Error extracting count and value fields", e);
            }

            // the aggregateValue is sum of all values against that
            // key/time. We therefore need to get the
            // average using the count column
            final double averagedValue = count != 0 ? (aggregatedValue / count) : 0;

            arr[idx] = ValDouble.create(averagedValue);
        };
        return extractor;
    }

    private ValueExtractor buildLongValueExtractor(final String columnName, final int fieldIndex) {
        return (rs, arr, cache) ->
                arr[fieldIndex] = getResultSetLong(rs, columnName);
    }

    private ValueExtractor buildTagFieldValueExtractor(final String fieldName, final int fieldIndex) {
        return (rs, arr, cache) -> {
            Val value = cache.get(fieldName);
            if (value == null) {
                //populate our cache of
                extractTagsMapFromColumn(getResultSetString(rs, SQLStatisticNames.NAME))
                        .forEach(cache::put);
            }
            value = cache.get(fieldName);
            arr[fieldIndex] = value;
        };
    }

    private Val getResultSetLong(final ResultSet resultSet, final String column) {
        try {
            return ValLong.create(resultSet.getLong(column));
        } catch (SQLException e) {
            throw new RuntimeException(String.format("Error extracting field %s", column), e);
        }
    }

    private Val getResultSetString(final ResultSet resultSet, final String column) {
        try {
            return ValString.create(resultSet.getString(column));
        } catch (SQLException e) {
            throw new RuntimeException(String.format("Error extracting field %s", column), e);
        }
    }

    private Flowable<Val[]> getFlowableQueryResults(final SqlBuilder sql,
                                                    final Function<ResultSet, Val[]> resultSetMapper) {

        //Not thread safe as each onNext will get the same ResultSet instance, however its position
        // will have mode on each time.
        Flowable<Val[]> resultSetFlowable = Flowable
                .using(
                        () -> new PreparedStatementResourceHolder(SQLStatisticsDbConnProvider, sql, searchConfig),
                        factory -> {
                            LOGGER.debug("Converting factory to a flowable");
                            Preconditions.checkNotNull(factory);
                            PreparedStatement ps = factory.getPreparedStatement();
                            return Flowable.generate(
                                    () -> {
                                        final Supplier<String> message = () -> "Executing query " + sql.toString();
                                        final TaskContext taskContext = taskContextProvider.get();
                                        taskContext.setName(SqlStatisticsStore.TASK_NAME);
                                        taskContext.info(message);
                                        LAMBDA_LOGGER.debug(message);

                                        try {
                                            return ps.executeQuery();
                                        } catch (SQLException e) {
                                            throw new RuntimeException(String.format("Error executing query %s, %s",
                                                    ps.toString(), e.getMessage()), e);
                                        }
                                    },
                                    (rs, emitter) -> {
                                        // The line below can be un-commented in development debugging to slow down the
                                        // return of all results to test iterative results and dashboard polling.
                                        // LockSupport.parkNanos(200_000);

                                        //advance the resultSet, if it is a row emit it, else finish the flow
                                        // TODO prob needs to change in 6.1
                                        if (Thread.currentThread().isInterrupted()) {
                                            LOGGER.debug("Task is terminated/interrupted, calling onComplete");
                                            emitter.onComplete();
                                        } else {
                                            if (rs.next()) {
                                                LOGGER.trace("calling onNext");
                                                Val[] values = resultSetMapper.apply(rs);
                                                emitter.onNext(values);
                                            } else {
                                                LOGGER.debug("End of resultSet, calling onComplete");
                                                emitter.onComplete();
                                            }
                                        }
                                    });
                        },
                        PreparedStatementResourceHolder::dispose);

        return resultSetFlowable;
    }

    /**
     * @param columnValue The value from the STAT_KEY.NAME column which could be of the
     *                    form 'StatName' or 'StatName¬Tag1¬Tag1Val1¬Tag2¬Tag2Val1'
     * @return A map of tag=>value, or an empty map if there are none
     */
    private Map<String, Val> extractTagsMapFromColumn(final Val columnValue) {
        final String[] tokens = columnValue.toString().split(SQLStatisticConstants.NAME_SEPARATOR);

        if (tokens.length == 1) {
            // no separators so there are no tags
            return Collections.emptyMap();
        } else if (tokens.length % 2 == 0) {
            throw new RuntimeException(
                    String.format("Expecting an odd number of tokens, columnValue: %s", columnValue));
        } else {
            final Map<String, Val> statisticTags = new HashMap<>();
            // stat name will be at pos 0 so start at 1
            for (int i = 1; i < tokens.length; i++) {
                final String tag = tokens[i++];
                String value = tokens[i];
                if (value.equals(SQLStatisticConstants.NULL_VALUE_STRING)) {
                    statisticTags.put(tag, ValNull.INSTANCE);
                } else {
                    statisticTags.put(tag, ValString.create(value));
                }
            }
            return statisticTags;
        }
    }

    /**
     * TODO: This is a bit simplistic as a user could create a filter that said
     * user=user1 AND user='*' which makes no sense. At the moment we would
     * assume that the user tag is being rolled up so user=user1 would never be
     * found in the data and thus would return no data.
     */
    private static RollUpBitMask buildRollUpBitMaskFromCriteria(final FindEventCriteria criteria,
                                                                final StatisticStoreDoc statisticsDataSource) {
        final Set<String> rolledUpTagsFound = criteria.getRolledUpFieldNames();

        final RollUpBitMask result;

        if (rolledUpTagsFound.size() > 0) {
            final List<Integer> rollUpTagPositionList = new ArrayList<>();

            for (final String tag : rolledUpTagsFound) {
                final Integer position = statisticsDataSource.getPositionInFieldList(tag);
                if (position == null) {
                    throw new RuntimeException(String.format("No field position found for tag %s", tag));
                }
                rollUpTagPositionList.add(position);
            }
            result = RollUpBitMask.fromTagPositions(rollUpTagPositionList);

        } else {
            result = RollUpBitMask.ZERO_MASK;
        }
        return result;
    }

    @FunctionalInterface
    private interface ValueExtractor {
        /**
         * Function for extracting values from a {@link ResultSet} and placing them into the passed String[]
         *
         * @param resultSet       The {@link ResultSet} instance to extract data from. It is assumed the {@link ResultSet}
         *                        has already been positioned at the desired row. next() should not be called on the
         *                        resultSet.
         * @param data            The data array to populate
         * @param fieldValueCache A map of fieldName=>fieldValue that can be used to hold state while
         *                        processing a row
         */
        void extract(final ResultSet resultSet,
                     final Val[] data,
                     final Map<String, Val> fieldValueCache);
    }

    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    private static class PreparedStatementResourceHolder {
        private Connection connection;
        private PreparedStatement preparedStatement;

        PreparedStatementResourceHolder(final DataSource dataSource,
                                        final SqlBuilder sql,
                                        final SearchConfig searchConfig) {
            try {
                connection = dataSource.getConnection();
            } catch (SQLException e) {
                throw new RuntimeException("Error getting connection", e);
            }
            try {
                //settings ot prevent mysql from reading the whole resultset into memory
                //see https://github.com/ontop/ontop/wiki/WorkingWithMySQL
                //Also needs 'useCursorFetch=true' on the jdbc connect string
                preparedStatement = connection.prepareStatement(
                        sql.toString(),
                        ResultSet.TYPE_FORWARD_ONLY,
                        ResultSet.CONCUR_READ_ONLY,
                        ResultSet.CLOSE_CURSORS_AT_COMMIT);

                final int fetchSize = searchConfig.getFetchSize();
                LOGGER.debug("Setting fetch size to {}", fetchSize);
                preparedStatement.setFetchSize(fetchSize);

                PreparedStatementUtil.setArguments(preparedStatement, sql.getArgs());
                LAMBDA_LOGGER.debug(() -> String.format("Created preparedStatement %s", preparedStatement.toString()));

            } catch (SQLException e) {
                throw new RuntimeException(String.format("Error preparing statement for sql [%s]", sql.toString()), e);
            }
        }

        PreparedStatement getPreparedStatement() {
            return preparedStatement;
        }

        void dispose() {
            LOGGER.debug("dispose called");
            if (preparedStatement != null) {
                try {
                    LOGGER.debug("Closing preparedStatement");
                    preparedStatement.close();
                } catch (SQLException e) {
                    throw new RuntimeException("Error closing preparedStatement", e);
                }
                preparedStatement = null;
            }
            if (connection != null) {
                try {
                    LOGGER.debug("Closing connection");
                    connection.close();
                } catch (SQLException e) {
                    throw new RuntimeException("Error closing connection", e);
                }
                connection = null;
            }
        }
    }

}
