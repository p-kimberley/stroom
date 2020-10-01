package stroom.searchable.impl;

import stroom.datasource.api.v2.AbstractField;
import stroom.entity.shared.ExpressionCriteria;
import stroom.query.api.v2.ExpressionOperator;
import stroom.query.api.v2.Param;
import stroom.query.api.v2.SearchRequest;
import stroom.query.common.v2.CompletionState;
import stroom.query.common.v2.Coprocessor;
import stroom.query.common.v2.CoprocessorFactory;
import stroom.query.common.v2.CoprocessorSettings;
import stroom.query.common.v2.CoprocessorSettingsFactory;
import stroom.query.common.v2.Data;
import stroom.query.common.v2.Payload;
import stroom.query.common.v2.PayloadFactory;
import stroom.query.common.v2.ResultHandler;
import stroom.query.common.v2.SearchResultHandler;
import stroom.query.common.v2.Sizes;
import stroom.query.common.v2.Store;
import stroom.query.common.v2.TablePayload;
import stroom.searchable.api.Searchable;
import stroom.task.api.TaskContext;
import stroom.task.api.TaskContextFactory;
import stroom.util.logging.LambdaLogger;
import stroom.util.logging.LambdaLoggerFactory;
import stroom.util.logging.LogUtil;

import com.google.common.base.Preconditions;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
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
    private final CoprocessorFactory coprocessorFactory;

    private final CompletionState completionState = new CompletionState();
    private final ResultHandler resultHandler;
    private final List<String> errors = Collections.synchronizedList(new ArrayList<>());
    private final AtomicBoolean terminate = new AtomicBoolean();
    private volatile Thread thread;

    SearchableStore(final Sizes defaultMaxResultsSizes,
                    final Sizes storeSize,
                    final int resultHandlerBatchSize,
                    final Searchable searchable,
                    final TaskContextFactory taskContextFactory,
                    final TaskContext taskContext,
                    final SearchRequest searchRequest,
                    final Executor executor,
                    final CoprocessorFactory coprocessorFactory) {
        this.defaultMaxResultsSizes = defaultMaxResultsSizes;
        this.storeSize = storeSize;
        this.coprocessorFactory = coprocessorFactory;

        searchKey = searchRequest.getKey().toString();
        LOGGER.debug(() -> LogUtil.message("Starting search with key {}", searchKey));
        taskContext.info(() -> "DB search " + searchKey + " - running query");

        // We want to get all common fields so find all unique field referenced in teh search request.
        final String[] mandatoryFieldNames = CoprocessorSettingsFactory.getAllRequiredFieldNames(searchRequest);
        final List<AbstractField> fields = searchable.getDataSource().getFields();
        final List<CoprocessorSettings> settingsList = CoprocessorSettingsFactory.create(
                searchRequest,
                fields,
                fields,
                List.of(mandatoryFieldNames));
        Preconditions.checkNotNull(settingsList);

        final Map<String, String> paramMap = getParamMap(searchRequest);

        final List<Coprocessor> coprocessors = getCoprocessors(settingsList);

        final ExpressionOperator expression = searchRequest.getQuery().getExpression();
        final ExpressionCriteria criteria = new ExpressionCriteria(expression);

        resultHandler = new SearchResultHandler(
                settingsList,
                defaultMaxResultsSizes,
                storeSize,
                paramMap);

        final Runnable runnable = taskContextFactory.context(taskContext, TASK_NAME, tc ->
                searchAsync(tc, searchable, criteria, settingsList.get(0).getFields(), coprocessors, resultHandlerBatchSize));
        CompletableFuture.runAsync(runnable, executor);
    }

    private void searchAsync(final TaskContext taskContext,
                             final Searchable searchable,
                             final ExpressionCriteria criteria,
                             final AbstractField[] fieldArray,
                             final List<Coprocessor> coprocessors,
                             final int resultHandlerBatchSize) {
        synchronized (SearchableStore.class) {
            thread = Thread.currentThread();
            if (terminate.get()) {
                return;
            }
        }

        final LongAdder counter = new LongAdder();
        final Instant queryStart = Instant.now();
        try {
            final AtomicLong nextProcessPayloadsTime = new AtomicLong(Instant.now().plus(RESULT_SEND_INTERVAL).toEpochMilli());
            final AtomicLong countSinceLastSend = new AtomicLong(0);


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
                coprocessors.forEach(coprocessor -> coprocessor.receive(data));
                // Send what we have every 1s or when the batch reaches a set size
                long now = System.currentTimeMillis();
                if (now >= nextProcessPayloadsTime.get() ||
                        countSinceLastSend.get() >= resultHandlerBatchSize) {

                    LOGGER.debug(() -> LogUtil.message("{} vs {}, {} vs {}",
                            now, nextProcessPayloadsTime,
                            countSinceLastSend.get(), resultHandlerBatchSize));

                    processPayloads(resultHandler, coprocessors);
                    taskContext.info(() -> searchKey +
                            " - running database query (" + counter.longValue() + " rows fetched)");
                    nextProcessPayloadsTime.set(Instant.now().plus(RESULT_SEND_INTERVAL).toEpochMilli());
                    countSinceLastSend.set(0);
                }
            });

        } catch (final RuntimeException e) {
            errors.add(e.getMessage());
        }

        LOGGER.debug(() ->
                String.format("complete called, counter: %s",
                        counter.longValue()));
        // completed our window so create and pass on a payload for the
        // data we have gathered so far
        processPayloads(resultHandler, coprocessors);
        taskContext.info(() -> searchKey + " - complete");

        LOGGER.debug(() -> "completeSearch called");
        complete();

        LOGGER.debug(() -> "Query finished in " + Duration.between(queryStart, Instant.now()));
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

    private List<Coprocessor> getCoprocessors(
            final List<CoprocessorSettings> settingsList) {

        return settingsList
                .stream()
                .map(coprocessorFactory::create)
                .collect(Collectors.toList());
    }

    /**
     * Synchronized to ensure multiple threads don't fight over the coprocessors which is unlikely to
     * happen anyway as it is mostly used in
     */
    private synchronized void processPayloads(final ResultHandler resultHandler,
                                              final List<Coprocessor> coprocessors) {

        if (!Thread.currentThread().isInterrupted()) {
            LOGGER.debug(() ->
                    LogUtil.message("processPayloads called for {} coprocessors", coprocessors.size()));

            // Build a payload map from whatever the coprocessors have in them, if anything
            final List<Payload> payloads = coprocessors.stream()
                    .map(PayloadFactory::createPayload)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            // log the queue sizes in the payload map
            LOGGER.debug(() -> {
                final String contents = payloads.stream()
                        .map(payload -> {
                            String key = payload.getKey() != null ? payload.getKey() : "null";
                            String size;
                            // entry checked for null in stream above
                            if (payload instanceof TablePayload) {
                                TablePayload tablePayload = (TablePayload) payload;
                                if (tablePayload.getSize() > 0) {
                                    size = Integer.toString(tablePayload.getSize());
                                } else {
                                    size = "null";
                                }
                            } else {
                                size = "?";
                            }
                            return key + ": " + size;
                        })
                        .collect(Collectors.joining(", "));
                return LogUtil.message("payloadMap: [{}]", contents);
            });

            // give the processed results to the collector, it will handle nulls
            resultHandler.handle(payloads);
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
    public void awaitCompletion() throws InterruptedException {
        completionState.awaitCompletion();
    }

    @Override
    public boolean awaitCompletion(final long timeout, final TimeUnit unit) throws InterruptedException {
        // Results are currently assembled synchronously in getMeta so the store is always complete.
        return completionState.awaitCompletion(timeout, unit);
    }

    @Override
    public Data getData(String componentId) {
        LOGGER.debug(() -> LogUtil.message("getMeta called for componentId {}", componentId));
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
