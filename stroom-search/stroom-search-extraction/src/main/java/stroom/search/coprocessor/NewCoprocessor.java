package stroom.search.coprocessor;

import stroom.dashboard.expression.v1.FieldIndexMap;
import stroom.query.common.v2.CoprocessorSettings;
import stroom.query.common.v2.CoprocessorSettingsMap.CoprocessorKey;
import stroom.query.common.v2.Payload;
import stroom.query.common.v2.PayloadFactory;

import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

public class NewCoprocessor implements Receiver, PayloadFactory {
    private final CoprocessorKey key;
    private final CoprocessorSettings settings;
    private final FieldIndexMap fieldIndexMap;
    private final Consumer<Values> valuesConsumer;
    private final Consumer<Error> errorConsumer;
    private final PayloadFactory payloadFactory;
    private final AtomicLong completionCount = new AtomicLong();

    NewCoprocessor(final CoprocessorKey key,
                   final CoprocessorSettings settings,
                   final FieldIndexMap fieldIndexMap,
                   final Consumer<Values> valuesConsumer,
                   final Consumer<Error> errorConsumer,
                   final PayloadFactory payloadFactory) {
        this.key = key;
        this.settings = settings;
        this.fieldIndexMap = fieldIndexMap;
        this.valuesConsumer = valuesConsumer;
        this.errorConsumer = errorConsumer;
        this.payloadFactory = payloadFactory;
    }

    public CoprocessorKey getKey() {
        return key;
    }

    public CoprocessorSettings getSettings() {
        return settings;
    }

    @Override
    public Consumer<Values> getValuesConsumer() {
        return valuesConsumer;
    }

    @Override
    public Consumer<Error> getErrorConsumer() {
        return errorConsumer;
    }

    @Override
    public Consumer<Long> getCompletionCountConsumer() {
        return completionCount::addAndGet;
    }

    @Override
    public FieldIndexMap getFieldIndexMap() {
        return fieldIndexMap;
    }

    @Override
    public Payload createPayload() {
        return payloadFactory.createPayload();
    }

    public long getCompletionCount() {
        return completionCount.get();
    }
}
