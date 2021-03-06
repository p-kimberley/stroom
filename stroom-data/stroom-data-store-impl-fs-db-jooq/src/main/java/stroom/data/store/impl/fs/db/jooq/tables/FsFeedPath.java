/*
 * This file is generated by jOOQ.
 */
package stroom.data.store.impl.fs.db.jooq.tables;


import stroom.data.store.impl.fs.db.jooq.Indexes;
import stroom.data.store.impl.fs.db.jooq.Keys;
import stroom.data.store.impl.fs.db.jooq.Stroom;
import stroom.data.store.impl.fs.db.jooq.tables.records.FsFeedPathRecord;

import org.jooq.Field;
import org.jooq.ForeignKey;
import org.jooq.Identity;
import org.jooq.Index;
import org.jooq.Name;
import org.jooq.Record;
import org.jooq.Row3;
import org.jooq.Schema;
import org.jooq.Table;
import org.jooq.TableField;
import org.jooq.UniqueKey;
import org.jooq.impl.DSL;
import org.jooq.impl.TableImpl;

import java.util.Arrays;
import java.util.List;
import javax.annotation.processing.Generated;


/**
 * This class is generated by jOOQ.
 */
@Generated(
    value = {
        "http://www.jooq.org",
        "jOOQ version:3.12.3"
    },
    comments = "This class is generated by jOOQ"
)
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class FsFeedPath extends TableImpl<FsFeedPathRecord> {

    private static final long serialVersionUID = 1368320918;

    /**
     * The reference instance of <code>stroom.fs_feed_path</code>
     */
    public static final FsFeedPath FS_FEED_PATH = new FsFeedPath();

    /**
     * The class holding records for this type
     */
    @Override
    public Class<FsFeedPathRecord> getRecordType() {
        return FsFeedPathRecord.class;
    }

    /**
     * The column <code>stroom.fs_feed_path.id</code>.
     */
    public final TableField<FsFeedPathRecord, Integer> ID = createField(DSL.name("id"), org.jooq.impl.SQLDataType.INTEGER.nullable(false).identity(true), this, "");

    /**
     * The column <code>stroom.fs_feed_path.name</code>.
     */
    public final TableField<FsFeedPathRecord, String> NAME = createField(DSL.name("name"), org.jooq.impl.SQLDataType.VARCHAR(255).nullable(false), this, "");

    /**
     * The column <code>stroom.fs_feed_path.path</code>.
     */
    public final TableField<FsFeedPathRecord, String> PATH = createField(DSL.name("path"), org.jooq.impl.SQLDataType.VARCHAR(255).nullable(false), this, "");

    /**
     * Create a <code>stroom.fs_feed_path</code> table reference
     */
    public FsFeedPath() {
        this(DSL.name("fs_feed_path"), null);
    }

    /**
     * Create an aliased <code>stroom.fs_feed_path</code> table reference
     */
    public FsFeedPath(String alias) {
        this(DSL.name(alias), FS_FEED_PATH);
    }

    /**
     * Create an aliased <code>stroom.fs_feed_path</code> table reference
     */
    public FsFeedPath(Name alias) {
        this(alias, FS_FEED_PATH);
    }

    private FsFeedPath(Name alias, Table<FsFeedPathRecord> aliased) {
        this(alias, aliased, null);
    }

    private FsFeedPath(Name alias, Table<FsFeedPathRecord> aliased, Field<?>[] parameters) {
        super(alias, null, aliased, parameters, DSL.comment(""));
    }

    public <O extends Record> FsFeedPath(Table<O> child, ForeignKey<O, FsFeedPathRecord> key) {
        super(child, key, FS_FEED_PATH);
    }

    @Override
    public Schema getSchema() {
        return Stroom.STROOM;
    }

    @Override
    public List<Index> getIndexes() {
        return Arrays.<Index>asList(Indexes.FS_FEED_PATH_NAME, Indexes.FS_FEED_PATH_PRIMARY);
    }

    @Override
    public Identity<FsFeedPathRecord, Integer> getIdentity() {
        return Keys.IDENTITY_FS_FEED_PATH;
    }

    @Override
    public UniqueKey<FsFeedPathRecord> getPrimaryKey() {
        return Keys.KEY_FS_FEED_PATH_PRIMARY;
    }

    @Override
    public List<UniqueKey<FsFeedPathRecord>> getKeys() {
        return Arrays.<UniqueKey<FsFeedPathRecord>>asList(Keys.KEY_FS_FEED_PATH_PRIMARY, Keys.KEY_FS_FEED_PATH_NAME);
    }

    @Override
    public FsFeedPath as(String alias) {
        return new FsFeedPath(DSL.name(alias), this);
    }

    @Override
    public FsFeedPath as(Name alias) {
        return new FsFeedPath(alias, this);
    }

    /**
     * Rename this table
     */
    @Override
    public FsFeedPath rename(String name) {
        return new FsFeedPath(DSL.name(name), null);
    }

    /**
     * Rename this table
     */
    @Override
    public FsFeedPath rename(Name name) {
        return new FsFeedPath(name, null);
    }

    // -------------------------------------------------------------------------
    // Row3 type methods
    // -------------------------------------------------------------------------

    @Override
    public Row3<Integer, String, String> fieldsRow() {
        return (Row3) super.fieldsRow();
    }
}
