package stroom.proxy.repo.dao;

import stroom.proxy.repo.ProxyRepoDbConnProvider;
import stroom.proxy.repo.db.jooq.tables.records.SourceEntryRecord;
import stroom.proxy.repo.db.jooq.tables.records.SourceItemRecord;
import stroom.util.logging.LambdaLogger;
import stroom.util.logging.LambdaLoggerFactory;

import org.jooq.Condition;
import org.jooq.Record1;
import org.jooq.SelectConditionStep;
import org.jooq.impl.DSL;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import javax.inject.Inject;
import javax.inject.Singleton;

import static stroom.proxy.repo.db.jooq.tables.AggregateItem.AGGREGATE_ITEM;
import static stroom.proxy.repo.db.jooq.tables.Source.SOURCE;
import static stroom.proxy.repo.db.jooq.tables.SourceEntry.SOURCE_ENTRY;
import static stroom.proxy.repo.db.jooq.tables.SourceItem.SOURCE_ITEM;

@Singleton
public class SourceEntryDao {

    private static final LambdaLogger LOGGER = LambdaLoggerFactory.getLogger(SourceEntryDao.class);

    // Find source items that have been aggregated but no longer have an associated aggregate.
    private static final SelectConditionStep<Record1<Long>> SELECT_SOURCE_ITEM_ID = DSL
            .select(SOURCE_ITEM.ID)
            .from(SOURCE_ITEM)
            .where(SOURCE_ITEM.AGGREGATED.isTrue())
            .andNotExists(DSL
                    .select(AGGREGATE_ITEM.ID)
                    .from(AGGREGATE_ITEM)
                    .where(AGGREGATE_ITEM.FK_SOURCE_ITEM_ID.eq(SOURCE_ITEM.ID)));
    private static final Condition DELETE_SOURCE_ENTRY_CONDITION =
            SOURCE_ENTRY.FK_SOURCE_ITEM_ID.in(SELECT_SOURCE_ITEM_ID);
    private static final Condition DELETE_SOURCE_ITEM_CONDITION =
            SOURCE_ITEM.ID.in(SELECT_SOURCE_ITEM_ID);

    private final SqliteJooqHelper jooq;

    private final AtomicLong sourceItemRecordId = new AtomicLong();
    private final AtomicLong sourceEntryRecordId = new AtomicLong();

    @Inject
    SourceEntryDao(final ProxyRepoDbConnProvider connProvider) {
        this.jooq = new SqliteJooqHelper(connProvider);
        init();
    }

    private void init() {
        final long maxSourceItemRecordId = jooq.getMaxId(SOURCE_ITEM, SOURCE_ITEM.ID).orElse(0L);
        sourceItemRecordId.set(maxSourceItemRecordId);

        final long maxSourceEntryRecordId = jooq.getMaxId(SOURCE_ENTRY, SOURCE_ENTRY.ID).orElse(0L);
        sourceEntryRecordId.set(maxSourceEntryRecordId);
    }

    public long nextSourceItemId() {
        return this.sourceItemRecordId.incrementAndGet();
    }

    public long nextSourceEntryId() {
        return this.sourceEntryRecordId.incrementAndGet();
    }

    /**
     * Delete all source entries and items.
     *
     * @return The number of rows changed.
     */
    public int deleteAll() {
        return jooq.contextResult(context -> {
            int total = 0;
            total += context
                    .deleteFrom(SOURCE_ENTRY)
                    .execute();
            total += context
                    .deleteFrom(SOURCE_ITEM)
                    .execute();
            return total;
        });
    }

    public void clear() {
        deleteAll();
        jooq
                .getMaxId(SOURCE_ENTRY, SOURCE_ENTRY.ID)
                .ifPresent(id -> {
                    throw new RuntimeException("Unexpected ID");
                });
        jooq
                .getMaxId(SOURCE_ITEM, SOURCE_ITEM.ID)
                .ifPresent(id -> {
                    throw new RuntimeException("Unexpected ID");
                });
        init();
    }

    /**
     * Delete source entries and items that have been added to aggregates but where the aggregates have since been
     * forwarded and deleted.
     *
     * @return The number of rows changed.
     */
    public int deleteUnused() {
        // Start a transaction for all of the database changes.
        return jooq.transactionResult(context -> {
            int count = context
                    .deleteFrom(SOURCE_ENTRY)
                    .where(DELETE_SOURCE_ENTRY_CONDITION)
                    .execute();

            count += context
                    .deleteFrom(SOURCE_ITEM)
                    .where(DELETE_SOURCE_ITEM_CONDITION)
                    .execute();

            return count;
        });
    }

    public int countItems() {
        return jooq.count(SOURCE_ITEM);
    }

    public int countEntries() {
        return jooq.count(SOURCE_ENTRY);
    }

    public void addEntries(final Path fullPath,
                           final long sourceId,
                           final Map<String, SourceItemRecord> itemNameMap,
                           final Map<Long, List<SourceEntryRecord>> entryMap) {
        jooq.transaction(context -> {
            final List<SourceItemRecord> sourceItemRecords = new ArrayList<>(itemNameMap.size());
            final List<SourceEntryRecord> sourceEntryRecords = new ArrayList<>();
            for (final SourceItemRecord sourceItemRecord : itemNameMap.values()) {
                if (sourceItemRecord.getFeedName() == null) {
                    LOGGER.error(() ->
                            "Source item has no feed name: " +
                                    fullPath +
                                    " - " +
                                    sourceItemRecord.getName());
                } else {
                    sourceItemRecords.add(sourceItemRecord);
                    final List<SourceEntryRecord> entries = entryMap.get(sourceItemRecord.getId());
                    sourceEntryRecords.addAll(entries);
                }
            }

            context.batchInsert(sourceItemRecords).execute();
            context.batchInsert(sourceEntryRecords).execute();

            // Mark the source as having been examined.
            context
                    .update(SOURCE)
                    .set(SOURCE.EXAMINED, true)
                    .where(SOURCE.ID.eq(sourceId))
                    .execute();
        });
    }

    public List<Long> getDeletableSourceItemIds() {
        return jooq.contextResult(context -> context
                .selectDistinct(SOURCE_ITEM.ID)
                .from(SOURCE_ITEM)
                .where(DELETE_SOURCE_ITEM_CONDITION)
                .fetch()
                .map(Record1::value1));
    }

    public List<Long> getDeletableSourceEntryIds() {
        return jooq.contextResult(context -> context
                .selectDistinct(SOURCE_ENTRY.ID)
                .from(SOURCE_ENTRY)
                .where(DELETE_SOURCE_ENTRY_CONDITION)
                .fetch()
                .map(Record1::value1));
    }

    public List<SourceItem> getNewSourceItems() {
        return getNewSourceItems(Integer.MAX_VALUE);
    }

    public List<SourceItem> getNewSourceItems(final int limit) {
        return jooq.contextResult(context -> context
                // Get all data items that have not been added to aggregate destinations.
                .select(SOURCE_ITEM.ID,
                        SOURCE_ITEM.FEED_NAME,
                        SOURCE_ITEM.TYPE_NAME,
                        DSL.sum(SOURCE_ENTRY.BYTE_SIZE))
                .from(SOURCE_ITEM)
                .join(SOURCE).on(SOURCE.ID.eq(SOURCE_ITEM.FK_SOURCE_ID))
                .join(SOURCE_ENTRY).on(SOURCE_ENTRY.FK_SOURCE_ITEM_ID.eq(SOURCE_ITEM.ID))
                .where(SOURCE_ITEM.AGGREGATED.isFalse())
                .groupBy(SOURCE_ITEM.ID)
                .orderBy(SOURCE.LAST_MODIFIED_TIME_MS, SOURCE.ID, SOURCE_ITEM.ID)
                .limit(limit)
                .fetch()
                .map(r -> new SourceItem(
                        r.value1(),
                        r.value2(),
                        r.value3(),
                        r.value4().longValue()
                )));
    }

    public List<SourceItem> getNewSourceItemsForSource(final long sourceId) {
        return jooq.contextResult(context -> context
                // Get all data items that have not been added to aggregate destinations.
                .select(SOURCE_ITEM.ID,
                        SOURCE_ITEM.FEED_NAME,
                        SOURCE_ITEM.TYPE_NAME,
                        DSL.sum(SOURCE_ENTRY.BYTE_SIZE))
                .from(SOURCE_ITEM)
                .join(SOURCE_ENTRY).on(SOURCE_ENTRY.FK_SOURCE_ITEM_ID.eq(SOURCE_ITEM.ID))
                .where(SOURCE_ITEM.FK_SOURCE_ID.eq(sourceId))
                .and(SOURCE_ITEM.AGGREGATED.isFalse())
                .groupBy(SOURCE_ITEM.ID)
                .orderBy(SOURCE_ITEM.ID)
                .fetch()
                .map(r -> new SourceItem(
                        r.value1(),
                        r.value2(),
                        r.value3(),
                        r.value4().longValue()
                )));
    }

    public static class SourceItem {

        private final long sourceItemId;
        private final String feedName;
        private final String typeName;
        private final long byteSize;

        public SourceItem(final long sourceItemId,
                          final String feedName,
                          final String typeName,
                          final long byteSize) {
            this.sourceItemId = sourceItemId;
            this.feedName = feedName;
            this.typeName = typeName;
            this.byteSize = byteSize;
        }

        public long getSourceItemId() {
            return sourceItemId;
        }

        public String getFeedName() {
            return feedName;
        }

        public String getTypeName() {
            return typeName;
        }

        public long getByteSize() {
            return byteSize;
        }
    }
}
