/*
 * This file is generated by jOOQ.
*/
package stroom.streamstore.meta.impl.db.stroom.tables;


import java.util.Arrays;
import java.util.List;

import javax.annotation.Generated;

import org.jooq.Field;
import org.jooq.Identity;
import org.jooq.Index;
import org.jooq.Name;
import org.jooq.Schema;
import org.jooq.Table;
import org.jooq.TableField;
import org.jooq.UniqueKey;
import org.jooq.impl.DSL;
import org.jooq.impl.TableImpl;

import stroom.streamstore.meta.impl.db.stroom.Indexes;
import stroom.streamstore.meta.impl.db.stroom.Keys;
import stroom.streamstore.meta.impl.db.stroom.Stroom;
import stroom.streamstore.meta.impl.db.stroom.tables.records.StreamProcessorRecord;


/**
 * This class is generated by jOOQ.
 */
@Generated(
    value = {
        "http://www.jooq.org",
        "jOOQ version:3.10.1"
    },
    comments = "This class is generated by jOOQ"
)
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class StreamProcessor extends TableImpl<StreamProcessorRecord> {

    private static final long serialVersionUID = 347134872;

    /**
     * The reference instance of <code>stroom.stream_processor</code>
     */
    public static final StreamProcessor STREAM_PROCESSOR = new StreamProcessor();

    /**
     * The class holding records for this type
     */
    @Override
    public Class<StreamProcessorRecord> getRecordType() {
        return StreamProcessorRecord.class;
    }

    /**
     * The column <code>stroom.stream_processor.id</code>.
     */
    public final TableField<StreamProcessorRecord, Integer> ID = createField("id", org.jooq.impl.SQLDataType.INTEGER.nullable(false).identity(true), this, "");

    /**
     * The column <code>stroom.stream_processor.pipeline_uuid</code>.
     */
    public final TableField<StreamProcessorRecord, String> PIPELINE_UUID = createField("pipeline_uuid", org.jooq.impl.SQLDataType.VARCHAR(255).nullable(false), this, "");

    /**
     * The column <code>stroom.stream_processor.processor_id</code>.
     */
    public final TableField<StreamProcessorRecord, Integer> PROCESSOR_ID = createField("processor_id", org.jooq.impl.SQLDataType.INTEGER.nullable(false), this, "");

    /**
     * Create a <code>stroom.stream_processor</code> table reference
     */
    public StreamProcessor() {
        this(DSL.name("stream_processor"), null);
    }

    /**
     * Create an aliased <code>stroom.stream_processor</code> table reference
     */
    public StreamProcessor(String alias) {
        this(DSL.name(alias), STREAM_PROCESSOR);
    }

    /**
     * Create an aliased <code>stroom.stream_processor</code> table reference
     */
    public StreamProcessor(Name alias) {
        this(alias, STREAM_PROCESSOR);
    }

    private StreamProcessor(Name alias, Table<StreamProcessorRecord> aliased) {
        this(alias, aliased, null);
    }

    private StreamProcessor(Name alias, Table<StreamProcessorRecord> aliased, Field<?>[] parameters) {
        super(alias, null, aliased, parameters, "");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Schema getSchema() {
        return Stroom.STROOM;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Index> getIndexes() {
        return Arrays.<Index>asList(Indexes.STREAM_PROCESSOR_PRIMARY);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Identity<StreamProcessorRecord, Integer> getIdentity() {
        return Keys.IDENTITY_STREAM_PROCESSOR;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public UniqueKey<StreamProcessorRecord> getPrimaryKey() {
        return Keys.KEY_STREAM_PROCESSOR_PRIMARY;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<UniqueKey<StreamProcessorRecord>> getKeys() {
        return Arrays.<UniqueKey<StreamProcessorRecord>>asList(Keys.KEY_STREAM_PROCESSOR_PRIMARY);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public StreamProcessor as(String alias) {
        return new StreamProcessor(DSL.name(alias), this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public StreamProcessor as(Name alias) {
        return new StreamProcessor(alias, this);
    }

    /**
     * Rename this table
     */
    @Override
    public StreamProcessor rename(String name) {
        return new StreamProcessor(DSL.name(name), null);
    }

    /**
     * Rename this table
     */
    @Override
    public StreamProcessor rename(Name name) {
        return new StreamProcessor(name, null);
    }
}
