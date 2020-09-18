package stroom.search.impl;

import stroom.query.common.v2.CompletionState;
import stroom.query.common.v2.Payload;
import stroom.search.coprocessor.Coprocessors;
import stroom.search.resultsender.NodeResult;
import stroom.task.api.TaskContext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

public class RemoteSearchResultFactory {
    private final CompletionState completionState = new CompletionState();
    private volatile Coprocessors coprocessors;
    private volatile LinkedBlockingQueue<String> errors;
    private volatile TaskContext taskContext;
    private volatile boolean destroy;
    private volatile boolean started;

    public NodeResult create() {
        if (!started) {
            return new NodeResult(Collections.emptyList(), Collections.emptyList(), false);
        } else if (Thread.currentThread().isInterrupted()) {
            completionState.complete();
            return new NodeResult(null, null, true);
        }

        // Find out if searching is complete.
        final boolean complete = completionState.isComplete();

        // Produce payloads for each coprocessor.
        final List<Payload> payloads = coprocessors.createPayloads();

        // Drain all current errors to a list.
        List<String> errorsSnapshot = new ArrayList<>();
        errors.drainTo(errorsSnapshot);
        if (errorsSnapshot.size() == 0) {
            errorsSnapshot = null;
        }

        // Form a result to send back to the requesting node.
        return new NodeResult(payloads, errorsSnapshot, complete);
    }

    public synchronized void destroy() {
        destroy = true;
        if (taskContext != null) {
            completionState.complete();
        }
    }

    public void setCoprocessors(final Coprocessors coprocessors) {
        this.coprocessors = coprocessors;
    }

    public CompletionState getCompletionState() {
        return completionState;
    }

    public void setErrors(final LinkedBlockingQueue<String> errors) {
        this.errors = errors;
    }

    public synchronized void setTaskContext(final TaskContext taskContext) {
        this.taskContext = taskContext;
        if (destroy) {
            completionState.complete();
        }
    }

    public void setStarted(final boolean started) {
        this.started = started;
    }
}
