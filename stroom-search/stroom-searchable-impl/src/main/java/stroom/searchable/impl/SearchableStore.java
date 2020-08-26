package stroom.searchable.impl;

import com.google.common.base.Functions;
import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import stroom.dashboard.expression.v1.FieldIndexMap;
import stroom.datasource.api.v2.DataSourceField;
import stroom.entity.shared.ExpressionCriteria;
import stroom.query.api.v2.ExpressionOperator;
import stroom.query.api.v2.Param;
import stroom.query.api.v2.SearchRequest;
import stroom.query.common.v2.CompletionState;
import stroom.query.common.v2.Coprocessor;
import stroom.query.common.v2.CoprocessorSettings;
import stroom.query.common.v2.CoprocessorSettingsMap;
import stroom.query.common.v2.Data;
import stroom.query.common.v2.Payload;
import stroom.query.common.v2.ResultHandler;
import stroom.query.common.v2.SearchResultHandler;
import stroom.query.common.v2.Sizes;
import stroom.query.common.v2.Store;
import stroom.query.common.v2.TableCoprocessor;
import stroom.query.common.v2.TableCoprocessorSettings;
import stroom.query.common.v2.TablePayload;
import stroom.searchable.api.Searchable;
import stroom.task.server.TaskContext;
import stroom.util.concurrent.ExecutorProvider;
import stroom.util.logging.LambdaLogger;
import stroom.util.logging.LambdaLoggerFactory;
import stroom.util.task.TaskWrapper;

import javax.inject.Provider;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;
import java.util.stream.Collectors;

class SearchableStore implements Store {
    private static final LambdaLogger LOGGER = LambdaLoggerFactory.getLogger(SearchableStore.class);

    private static final Duration RESULT_SEND_INTERVAL = Duration.ofSeconds(1);

    private static final String TASK_NAME = "DB Search";

    private final Sizes defaultMaxResultsSizes;
    private final Sizes storeSize;
    private final String searchKey;
    private final TaskContext taskContext;
    private final CompletionState completionState = new CompletionState();

    private final ResultHandler resultHandler;
    private final List<String> errors = Collections.synchronizedList(new ArrayList<>());
    private final AtomicBoolean terminate = new AtomicBoolean();
    private volatile Thread thread;

    SearchableStore(final Sizes defaultMaxResultsSizes,
                    final Sizes storeSize,
                    final int resultHandlerBatchSize,
                    final Searchable searchable,
                    final TaskContext taskContext,
                    final SearchRequest searchRequest,
                    final ExecutorProvider executorProvider,
                    final Provider<TaskWrapper> taskWrapperProvider) {
        this.defaultMaxResultsSizes = defaultMaxResultsSizes;
        this.storeSize = storeSize;
        this.taskContext = taskContext;

        searchKey = searchRequest.getKey().toString();
        LOGGER.debug(() -> "Starting search with key " + searchKey);
        taskContext.setName(TASK_NAME);
        taskContext.info("DB search " + searchKey + " - running query");

        final CoprocessorSettingsMap coprocessorSettingsMap = CoprocessorSettingsMap.create(searchRequest);
        Preconditions.checkNotNull(coprocessorSettingsMap);

        final FieldIndexMap fieldIndexMap = new FieldIndexMap(true);
        final Map<String, String> paramMap = getParamMap(searchRequest);

        final Map<CoprocessorSettingsMap.CoprocessorKey, Coprocessor> coprocessorMap = getCoprocessorMap(
                coprocessorSettingsMap, fieldIndexMap, paramMap);

        final ExpressionOperator expression = searchRequest.getQuery().getExpression();
        final ExpressionCriteria criteria = new ExpressionCriteria(expression);

        resultHandler = new SearchResultHandler(coprocessorSettingsMap, defaultMaxResultsSizes, storeSize);

        final Map<String, DataSourceField> fieldMap = searchable.getDataSource().getFields()
                .stream()
                .collect(Collectors.toMap(DataSourceField::getName, Functions.identity()));
        final OptionalInt max = fieldIndexMap.getMap().values().stream().mapToInt(Integer::intValue).max();
        final DataSourceField[] fieldArray = new DataSourceField[max.orElse(-1) + 1];
        fieldIndexMap.getMap().forEach((k, v) -> {
            final DataSourceField field = fieldMap.get(k);
            fieldArray[v] = field;
        });

        Runnable runnable = () -> {
            synchronized (SearchableStore.class) {
                thread = Thread.currentThread();
                if (terminate.get()) {
                    return;
                }
            }

            final LongAdder counter = new LongAdder();
            final AtomicLong nextProcessPayloadsTime = new AtomicLong(Instant.now().plus(RESULT_SEND_INTERVAL).toEpochMilli());
            final AtomicLong countSinceLastSend = new AtomicLong(0);
            final Instant queryStart = Instant.now();

            searchable.search(criteria, fieldArray, data -> {
//                final Val[] data = new Val[v.length];
//                for (int i = 0; i < v.length; i++) {
//                    Val val = ValNull.INSTANCE;
//                    final Object o = v[i];
//                    if (o instanceof Integer) {
//                        val = ValInteger.create((Integer) o);
//                    } else if (o instanceof Double) {
//                        val = ValDouble.create((Double) o);
//                    } else if (o instanceof Long) {
//                        val = ValLong.create((Long) o);
//                    } else if (o instanceof String) {
//                        val = ValString.create((String) o);
//                    } else if (o instanceof Boolean) {
//                        val = ValBoolean.create((Boolean) o);
//                    }
//                    data[i] = val;
//                }

                counter.increment();
                countSinceLastSend.incrementAndGet();
                LOGGER.trace(() -> String.format("data: [%s]", Arrays.toString(data)));

                // Give the data array to each of our coprocessors
                coprocessorMap.values().forEach(coprocessor ->
                        coprocessor.receive(data));
                // Send what we have every 1s or when the batch reaches a set size
                long now = System.currentTimeMillis();
                if (now >= nextProcessPayloadsTime.get() ||
                        countSinceLastSend.get() >= resultHandlerBatchSize) {

                    LOGGER.debug(() -> now + " vs " + nextProcessPayloadsTime + ", " + countSinceLastSend.get() + " vs " + resultHandlerBatchSize);

                    processPayloads(resultHandler, coprocessorMap);
                    taskContext.setName(TASK_NAME);
                    taskContext.info(searchKey +
                            " - running database query (" + counter.longValue() + " rows fetched)");
                    nextProcessPayloadsTime.set(Instant.now().plus(RESULT_SEND_INTERVAL).toEpochMilli());
                    countSinceLastSend.set(0);
                }
            });

            LOGGER.debug(() ->
                    String.format("complete called, counter: %s",
                            counter.longValue()));
            // completed our window so create and pass on a payload for the
            // data we have gathered so far
            processPayloads(resultHandler, coprocessorMap);
            taskContext.info(searchKey + " - complete");
            LOGGER.debug(() -> "completeSearch called");
            resultHandler.waitForPendingWork(taskContext);
            complete();

            LOGGER.debug(() -> "Query finished in " + Duration.between(queryStart, Instant.now()));
        };
        runnable = taskWrapperProvider.get().wrap(runnable);

        executorProvider.getExecutor().execute(runnable);
    }

    private Map<String, String> getParamMap(final SearchRequest searchRequest) {
        final Map<String, String> paramMap;
        if (searchRequest.getQuery().getParams() != null) {
            paramMap = searchRequest.getQuery().getParams().stream()
                    .collect(Collectors.toMap(Param::getKey, Param::getValue));
        } else {
            paramMap = Collections.emptyMap();
        }
        return paramMap;
    }

    private Map<CoprocessorSettingsMap.CoprocessorKey, Coprocessor> getCoprocessorMap(
            final CoprocessorSettingsMap coprocessorSettingsMap,
            final FieldIndexMap fieldIndexMap,
            final Map<String, String> paramMap) {

        return coprocessorSettingsMap.getMap()
                .entrySet()
                .stream()
                .map(entry -> Maps.immutableEntry(
                        entry.getKey(),
                        createCoprocessor(entry.getValue(), fieldIndexMap, paramMap)))
                .filter(entry -> entry.getKey() != null)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private Coprocessor createCoprocessor(final CoprocessorSettings settings,
                                          final FieldIndexMap fieldIndexMap,
                                          final Map<String, String> paramMap) {
        if (settings instanceof TableCoprocessorSettings) {
            final TableCoprocessorSettings tableCoprocessorSettings = (TableCoprocessorSettings) settings;
            return new TableCoprocessor(tableCoprocessorSettings, fieldIndexMap, taskContext, paramMap);
        }
        return null;
    }

    /**
     * Synchronized to ensure multiple threads don't fight over the coprocessors which is unlikely to
     * happen anyway as it is mostly used in
     */
    private synchronized void processPayloads(final ResultHandler resultHandler,
                                              final Map<CoprocessorSettingsMap.CoprocessorKey, Coprocessor> coprocessorMap) {

        if (!Thread.currentThread().isInterrupted()) {
            LOGGER.debug(() -> "processPayloads called for " + coprocessorMap.size() + " coprocessors");

            //build a payload map from whatever the coprocessors have in them, if anything
            final Map<CoprocessorSettingsMap.CoprocessorKey, Payload> payloadMap = coprocessorMap.entrySet().stream()
                    .map(entry ->
                            Maps.immutableEntry(entry.getKey(), entry.getValue().createPayload()))
                    .filter(entry ->
                            entry.getValue() != null)
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

            // log the queue sizes in the payload map
            LOGGER.debug(() -> {
                final String contents = payloadMap.entrySet().stream()
                        .map(entry -> {
                            String key = entry.getKey() != null ? entry.getKey().toString() : "null";
                            String size;
                            // entry checked for null in stream above
                            if (entry.getValue() instanceof TablePayload) {
                                TablePayload tablePayload = (TablePayload) entry.getValue();
                                if (tablePayload.getQueue() != null) {
                                    size = Integer.toString(tablePayload.getQueue().size());
                                } else {
                                    size = "null";
                                }
                            } else {
                                size = "?";
                            }
                            return key + ": " + size;
                        })
                        .collect(Collectors.joining(", "));
                return "payloadMap: [" + contents + "]";
            });

            // give the processed results to the collector, it will handle nulls
            resultHandler.handle(payloadMap, taskContext);
        } else {
            LOGGER.debug(() -> "Thread is interrupted, not processing payload");
        }
    }


    @Override
    public void destroy() {
        synchronized (SearchableStore.class) {
            LOGGER.debug(() -> "destroy called");
            // Terminate the search
            terminate.set(true);
            if (thread != null) {
                thread.interrupt();
            }
            complete();
        }
    }

    public void complete() {
        completionState.complete();
    }

    @Override
    public boolean isComplete() {
        return completionState.isComplete();
    }

    @Override
    public boolean awaitCompletion(final long timeout, final TimeUnit unit) throws InterruptedException {
        return completionState.awaitCompletion(timeout, unit);
    }

    @Override
    public Data getData(String componentId) {
        LOGGER.debug(() -> "getMeta called for componentId " + componentId);
        return resultHandler.getResultStore(componentId);
    }

    @Override
    public List<String> getErrors() {
        return errors;
    }

    @Override
    public List<String> getHighlights() {
        return null;
    }

    @Override
    public Sizes getDefaultMaxResultsSizes() {
        return defaultMaxResultsSizes;
    }

    @Override
    public Sizes getStoreSize() {
        return storeSize;
    }

    @Override
    public String toString() {
        return "DbStore{" +
                "defaultMaxResultsSizes=" + defaultMaxResultsSizes +
                ", storeSize=" + storeSize +
                ", completionState=" + completionState +
//                ", isTerminated=" + isTerminated +
                ", searchKey='" + searchKey + '\'' +
                '}';
    }
}
