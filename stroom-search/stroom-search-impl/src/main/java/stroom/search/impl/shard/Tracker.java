package stroom.search.impl.shard;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

class Tracker {
    private static final Logger LOGGER = LoggerFactory.getLogger(Tracker.class);

    private final AtomicLong hitCount = new AtomicLong();
    private final AtomicBoolean completed = new AtomicBoolean();

    long getHitCount() {
        return hitCount.get();
    }

    void incrementHitCount() {
        hitCount.incrementAndGet();
    }

    boolean isCompleted() {
        return completed.get();
    }

    void complete() {
        LOGGER.info("TRACKER COMPLETE with " + hitCount.get() + " hits");
        completed.set(true);
    }
}
