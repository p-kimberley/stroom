/*
 * This file is generated by jOOQ.
 */
package stroom.index.impl.db.tables.records;


import org.jooq.Field;
import org.jooq.Record1;
import org.jooq.Record19;
import org.jooq.Row19;
import org.jooq.impl.UpdatableRecordImpl;
import stroom.index.impl.db.tables.IndexShard;

import javax.annotation.Generated;


/**
 * This class is generated by jOOQ.
 */
@Generated(
    value = {
        "http://www.jooq.org",
        "jOOQ version:3.11.9"
    },
    comments = "This class is generated by jOOQ"
)
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class IndexShardRecord extends UpdatableRecordImpl<IndexShardRecord> implements Record19<Long, Byte, String, Long, String, Long, String, Long, String, Integer, Long, Long, Integer, Long, Byte, String, String, Long, Long> {

    private static final long serialVersionUID = 1956327698;

    /**
     * Setter for <code>stroom.index_shard.id</code>.
     */
    public void setId(Long value) {
        set(0, value);
    }

    /**
     * Getter for <code>stroom.index_shard.id</code>.
     */
    public Long getId() {
        return (Long) get(0);
    }

    /**
     * Setter for <code>stroom.index_shard.version</code>.
     */
    public void setVersion(Byte value) {
        set(1, value);
    }

    /**
     * Getter for <code>stroom.index_shard.version</code>.
     */
    public Byte getVersion() {
        return (Byte) get(1);
    }

    /**
     * Setter for <code>stroom.index_shard.node_name</code>.
     */
    public void setNodeName(String value) {
        set(2, value);
    }

    /**
     * Getter for <code>stroom.index_shard.node_name</code>.
     */
    public String getNodeName() {
        return (String) get(2);
    }

    /**
     * Setter for <code>stroom.index_shard.fk_volume_id</code>.
     */
    public void setFkVolumeId(Long value) {
        set(3, value);
    }

    /**
     * Getter for <code>stroom.index_shard.fk_volume_id</code>.
     */
    public Long getFkVolumeId() {
        return (Long) get(3);
    }

    /**
     * Setter for <code>stroom.index_shard.index_uuid</code>.
     */
    public void setIndexUuid(String value) {
        set(4, value);
    }

    /**
     * Getter for <code>stroom.index_shard.index_uuid</code>.
     */
    public String getIndexUuid() {
        return (String) get(4);
    }

    /**
     * Setter for <code>stroom.index_shard.create_time_ms</code>.
     */
    public void setCreateTimeMs(Long value) {
        set(5, value);
    }

    /**
     * Getter for <code>stroom.index_shard.create_time_ms</code>.
     */
    public Long getCreateTimeMs() {
        return (Long) get(5);
    }

    /**
     * Setter for <code>stroom.index_shard.create_user</code>.
     */
    public void setCreateUser(String value) {
        set(6, value);
    }

    /**
     * Getter for <code>stroom.index_shard.create_user</code>.
     */
    public String getCreateUser() {
        return (String) get(6);
    }

    /**
     * Setter for <code>stroom.index_shard.update_time_ms</code>.
     */
    public void setUpdateTimeMs(Long value) {
        set(7, value);
    }

    /**
     * Getter for <code>stroom.index_shard.update_time_ms</code>.
     */
    public Long getUpdateTimeMs() {
        return (Long) get(7);
    }

    /**
     * Setter for <code>stroom.index_shard.update_user</code>.
     */
    public void setUpdateUser(String value) {
        set(8, value);
    }

    /**
     * Getter for <code>stroom.index_shard.update_user</code>.
     */
    public String getUpdateUser() {
        return (String) get(8);
    }

    /**
     * Setter for <code>stroom.index_shard.commit_doc_count</code>.
     */
    public void setCommitDocCount(Integer value) {
        set(9, value);
    }

    /**
     * Getter for <code>stroom.index_shard.commit_doc_count</code>.
     */
    public Integer getCommitDocCount() {
        return (Integer) get(9);
    }

    /**
     * Setter for <code>stroom.index_shard.commit_duration_ms</code>.
     */
    public void setCommitDurationMs(Long value) {
        set(10, value);
    }

    /**
     * Getter for <code>stroom.index_shard.commit_duration_ms</code>.
     */
    public Long getCommitDurationMs() {
        return (Long) get(10);
    }

    /**
     * Setter for <code>stroom.index_shard.commit_ms</code>.
     */
    public void setCommitMs(Long value) {
        set(11, value);
    }

    /**
     * Getter for <code>stroom.index_shard.commit_ms</code>.
     */
    public Long getCommitMs() {
        return (Long) get(11);
    }

    /**
     * Setter for <code>stroom.index_shard.doc_count</code>.
     */
    public void setDocCount(Integer value) {
        set(12, value);
    }

    /**
     * Getter for <code>stroom.index_shard.doc_count</code>.
     */
    public Integer getDocCount() {
        return (Integer) get(12);
    }

    /**
     * Setter for <code>stroom.index_shard.file_size</code>.
     */
    public void setFileSize(Long value) {
        set(13, value);
    }

    /**
     * Getter for <code>stroom.index_shard.file_size</code>.
     */
    public Long getFileSize() {
        return (Long) get(13);
    }

    /**
     * Setter for <code>stroom.index_shard.status</code>.
     */
    public void setStatus(Byte value) {
        set(14, value);
    }

    /**
     * Getter for <code>stroom.index_shard.status</code>.
     */
    public Byte getStatus() {
        return (Byte) get(14);
    }

    /**
     * Setter for <code>stroom.index_shard.partition</code>.
     */
    public void setPartition(String value) {
        set(15, value);
    }

    /**
     * Getter for <code>stroom.index_shard.partition</code>.
     */
    public String getPartition() {
        return (String) get(15);
    }

    /**
     * Setter for <code>stroom.index_shard.index_version</code>.
     */
    public void setIndexVersion(String value) {
        set(16, value);
    }

    /**
     * Getter for <code>stroom.index_shard.index_version</code>.
     */
    public String getIndexVersion() {
        return (String) get(16);
    }

    /**
     * Setter for <code>stroom.index_shard.partition_from_ms</code>.
     */
    public void setPartitionFromMs(Long value) {
        set(17, value);
    }

    /**
     * Getter for <code>stroom.index_shard.partition_from_ms</code>.
     */
    public Long getPartitionFromMs() {
        return (Long) get(17);
    }

    /**
     * Setter for <code>stroom.index_shard.partition_to_ms</code>.
     */
    public void setPartitionToMs(Long value) {
        set(18, value);
    }

    /**
     * Getter for <code>stroom.index_shard.partition_to_ms</code>.
     */
    public Long getPartitionToMs() {
        return (Long) get(18);
    }

    // -------------------------------------------------------------------------
    // Primary key information
    // -------------------------------------------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public Record1<Long> key() {
        return (Record1) super.key();
    }

    // -------------------------------------------------------------------------
    // Record19 type implementation
    // -------------------------------------------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public Row19<Long, Byte, String, Long, String, Long, String, Long, String, Integer, Long, Long, Integer, Long, Byte, String, String, Long, Long> fieldsRow() {
        return (Row19) super.fieldsRow();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Row19<Long, Byte, String, Long, String, Long, String, Long, String, Integer, Long, Long, Integer, Long, Byte, String, String, Long, Long> valuesRow() {
        return (Row19) super.valuesRow();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Field<Long> field1() {
        return IndexShard.INDEX_SHARD.ID;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Field<Byte> field2() {
        return IndexShard.INDEX_SHARD.VERSION;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Field<String> field3() {
        return IndexShard.INDEX_SHARD.NODE_NAME;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Field<Long> field4() {
        return IndexShard.INDEX_SHARD.FK_VOLUME_ID;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Field<String> field5() {
        return IndexShard.INDEX_SHARD.INDEX_UUID;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Field<Long> field6() {
        return IndexShard.INDEX_SHARD.CREATE_TIME_MS;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Field<String> field7() {
        return IndexShard.INDEX_SHARD.CREATE_USER;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Field<Long> field8() {
        return IndexShard.INDEX_SHARD.UPDATE_TIME_MS;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Field<String> field9() {
        return IndexShard.INDEX_SHARD.UPDATE_USER;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Field<Integer> field10() {
        return IndexShard.INDEX_SHARD.COMMIT_DOC_COUNT;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Field<Long> field11() {
        return IndexShard.INDEX_SHARD.COMMIT_DURATION_MS;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Field<Long> field12() {
        return IndexShard.INDEX_SHARD.COMMIT_MS;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Field<Integer> field13() {
        return IndexShard.INDEX_SHARD.DOC_COUNT;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Field<Long> field14() {
        return IndexShard.INDEX_SHARD.FILE_SIZE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Field<Byte> field15() {
        return IndexShard.INDEX_SHARD.STATUS;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Field<String> field16() {
        return IndexShard.INDEX_SHARD.PARTITION;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Field<String> field17() {
        return IndexShard.INDEX_SHARD.INDEX_VERSION;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Field<Long> field18() {
        return IndexShard.INDEX_SHARD.PARTITION_FROM_MS;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Field<Long> field19() {
        return IndexShard.INDEX_SHARD.PARTITION_TO_MS;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Long component1() {
        return getId();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Byte component2() {
        return getVersion();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String component3() {
        return getNodeName();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Long component4() {
        return getFkVolumeId();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String component5() {
        return getIndexUuid();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Long component6() {
        return getCreateTimeMs();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String component7() {
        return getCreateUser();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Long component8() {
        return getUpdateTimeMs();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String component9() {
        return getUpdateUser();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Integer component10() {
        return getCommitDocCount();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Long component11() {
        return getCommitDurationMs();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Long component12() {
        return getCommitMs();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Integer component13() {
        return getDocCount();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Long component14() {
        return getFileSize();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Byte component15() {
        return getStatus();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String component16() {
        return getPartition();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String component17() {
        return getIndexVersion();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Long component18() {
        return getPartitionFromMs();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Long component19() {
        return getPartitionToMs();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Long value1() {
        return getId();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Byte value2() {
        return getVersion();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String value3() {
        return getNodeName();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Long value4() {
        return getFkVolumeId();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String value5() {
        return getIndexUuid();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Long value6() {
        return getCreateTimeMs();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String value7() {
        return getCreateUser();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Long value8() {
        return getUpdateTimeMs();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String value9() {
        return getUpdateUser();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Integer value10() {
        return getCommitDocCount();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Long value11() {
        return getCommitDurationMs();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Long value12() {
        return getCommitMs();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Integer value13() {
        return getDocCount();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Long value14() {
        return getFileSize();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Byte value15() {
        return getStatus();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String value16() {
        return getPartition();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String value17() {
        return getIndexVersion();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Long value18() {
        return getPartitionFromMs();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Long value19() {
        return getPartitionToMs();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IndexShardRecord value1(Long value) {
        setId(value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IndexShardRecord value2(Byte value) {
        setVersion(value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IndexShardRecord value3(String value) {
        setNodeName(value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IndexShardRecord value4(Long value) {
        setFkVolumeId(value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IndexShardRecord value5(String value) {
        setIndexUuid(value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IndexShardRecord value6(Long value) {
        setCreateTimeMs(value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IndexShardRecord value7(String value) {
        setCreateUser(value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IndexShardRecord value8(Long value) {
        setUpdateTimeMs(value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IndexShardRecord value9(String value) {
        setUpdateUser(value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IndexShardRecord value10(Integer value) {
        setCommitDocCount(value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IndexShardRecord value11(Long value) {
        setCommitDurationMs(value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IndexShardRecord value12(Long value) {
        setCommitMs(value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IndexShardRecord value13(Integer value) {
        setDocCount(value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IndexShardRecord value14(Long value) {
        setFileSize(value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IndexShardRecord value15(Byte value) {
        setStatus(value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IndexShardRecord value16(String value) {
        setPartition(value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IndexShardRecord value17(String value) {
        setIndexVersion(value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IndexShardRecord value18(Long value) {
        setPartitionFromMs(value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IndexShardRecord value19(Long value) {
        setPartitionToMs(value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IndexShardRecord values(Long value1, Byte value2, String value3, Long value4, String value5, Long value6, String value7, Long value8, String value9, Integer value10, Long value11, Long value12, Integer value13, Long value14, Byte value15, String value16, String value17, Long value18, Long value19) {
        value1(value1);
        value2(value2);
        value3(value3);
        value4(value4);
        value5(value5);
        value6(value6);
        value7(value7);
        value8(value8);
        value9(value9);
        value10(value10);
        value11(value11);
        value12(value12);
        value13(value13);
        value14(value14);
        value15(value15);
        value16(value16);
        value17(value17);
        value18(value18);
        value19(value19);
        return this;
    }

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    /**
     * Create a detached IndexShardRecord
     */
    public IndexShardRecord() {
        super(IndexShard.INDEX_SHARD);
    }

    /**
     * Create a detached, initialised IndexShardRecord
     */
    public IndexShardRecord(Long id, Byte version, String nodeName, Long fkVolumeId, String indexUuid, Long createTimeMs, String createUser, Long updateTimeMs, String updateUser, Integer commitDocCount, Long commitDurationMs, Long commitMs, Integer docCount, Long fileSize, Byte status, String partition, String indexVersion, Long partitionFromMs, Long partitionToMs) {
        super(IndexShard.INDEX_SHARD);

        set(0, id);
        set(1, version);
        set(2, nodeName);
        set(3, fkVolumeId);
        set(4, indexUuid);
        set(5, createTimeMs);
        set(6, createUser);
        set(7, updateTimeMs);
        set(8, updateUser);
        set(9, commitDocCount);
        set(10, commitDurationMs);
        set(11, commitMs);
        set(12, docCount);
        set(13, fileSize);
        set(14, status);
        set(15, partition);
        set(16, indexVersion);
        set(17, partitionFromMs);
        set(18, partitionToMs);
    }
}
