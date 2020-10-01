package stroom.search.coprocessor;

import stroom.datasource.api.v2.AbstractField;

import java.util.function.Consumer;

public class ReceiverImpl implements Receiver {
    private final Consumer<Values> valuesConsumer;
    private final Consumer<Error> errorConsumer;
    private final Consumer<Long> completionCountConsumer;
    private final AbstractField[] fields;

    public ReceiverImpl(final Consumer<Values> valuesConsumer,
                        final Consumer<Error> errorConsumer,
                        final Consumer<Long> completionCountConsumer,
                        final AbstractField[] fields) {
        this.valuesConsumer = valuesConsumer;
        this.errorConsumer = errorConsumer;
        this.completionCountConsumer = completionCountConsumer;
        this.fields = fields;
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
        return completionCountConsumer;
    }

    @Override
    public AbstractField[] getFields() {
        return fields;
    }
}