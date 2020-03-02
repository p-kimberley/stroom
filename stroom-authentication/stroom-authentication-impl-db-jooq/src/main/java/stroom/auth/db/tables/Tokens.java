/*
 * This file is generated by jOOQ.
 */
package stroom.auth.db.tables;


import java.sql.Timestamp;
import java.util.Arrays;
import java.util.List;

import javax.annotation.Generated;

import org.jooq.Field;
import org.jooq.ForeignKey;
import org.jooq.Identity;
import org.jooq.Index;
import org.jooq.Name;
import org.jooq.Record;
import org.jooq.Schema;
import org.jooq.Table;
import org.jooq.TableField;
import org.jooq.UniqueKey;
import org.jooq.impl.DSL;
import org.jooq.impl.TableImpl;

import stroom.auth.db.Auth;
import stroom.auth.db.Indexes;
import stroom.auth.db.Keys;
import stroom.auth.db.tables.records.TokensRecord;


/**
 * This class is generated by jOOQ.
 */
@Generated(
    value = {
        "http://www.jooq.org",
        "jOOQ version:3.11.7"
    },
    comments = "This class is generated by jOOQ"
)
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class Tokens extends TableImpl<TokensRecord> {

    private static final long serialVersionUID = 715091897;

    /**
     * The reference instance of <code>auth.tokens</code>
     */
    public static final Tokens TOKENS = new Tokens();

    /**
     * The class holding records for this type
     */
    @Override
    public Class<TokensRecord> getRecordType() {
        return TokensRecord.class;
    }

    /**
     * The column <code>auth.tokens.id</code>.
     */
    public final TableField<TokensRecord, Integer> ID = createField("id", org.jooq.impl.SQLDataType.INTEGER.nullable(false).identity(true), this, "");

    /**
     * The column <code>auth.tokens.user_id</code>.
     */
    public final TableField<TokensRecord, Integer> USER_ID = createField("user_id", org.jooq.impl.SQLDataType.INTEGER.nullable(false), this, "");

    /**
     * The column <code>auth.tokens.token_type_id</code>.
     */
    public final TableField<TokensRecord, Integer> TOKEN_TYPE_ID = createField("token_type_id", org.jooq.impl.SQLDataType.INTEGER.nullable(false), this, "");

    /**
     * The column <code>auth.tokens.token</code>.
     */
    public final TableField<TokensRecord, String> TOKEN = createField("token", org.jooq.impl.SQLDataType.VARCHAR(1000).nullable(false), this, "");

    /**
     * The column <code>auth.tokens.expires_on</code>.
     */
    public final TableField<TokensRecord, Timestamp> EXPIRES_ON = createField("expires_on", org.jooq.impl.SQLDataType.TIMESTAMP, this, "");

    /**
     * The column <code>auth.tokens.comments</code>.
     */
    public final TableField<TokensRecord, String> COMMENTS = createField("comments", org.jooq.impl.SQLDataType.VARCHAR(500), this, "");

    /**
     * The column <code>auth.tokens.issued_on</code>.
     */
    public final TableField<TokensRecord, Timestamp> ISSUED_ON = createField("issued_on", org.jooq.impl.SQLDataType.TIMESTAMP.nullable(false).defaultValue(org.jooq.impl.DSL.inline("0000-00-00 00:00:00", org.jooq.impl.SQLDataType.TIMESTAMP)), this, "");

    /**
     * The column <code>auth.tokens.issued_by_user</code>.
     */
    public final TableField<TokensRecord, Integer> ISSUED_BY_USER = createField("issued_by_user", org.jooq.impl.SQLDataType.INTEGER, this, "");

    /**
     * The column <code>auth.tokens.enabled</code>.
     */
    public final TableField<TokensRecord, Boolean> ENABLED = createField("enabled", org.jooq.impl.SQLDataType.BIT.defaultValue(org.jooq.impl.DSL.inline("b'1'", org.jooq.impl.SQLDataType.BIT)), this, "");

    /**
     * The column <code>auth.tokens.updated_on</code>.
     */
    public final TableField<TokensRecord, Timestamp> UPDATED_ON = createField("updated_on", org.jooq.impl.SQLDataType.TIMESTAMP, this, "");

    /**
     * The column <code>auth.tokens.updated_by_user</code>.
     */
    public final TableField<TokensRecord, Integer> UPDATED_BY_USER = createField("updated_by_user", org.jooq.impl.SQLDataType.INTEGER, this, "");

    /**
     * Create a <code>auth.tokens</code> table reference
     */
    public Tokens() {
        this(DSL.name("tokens"), null);
    }

    /**
     * Create an aliased <code>auth.tokens</code> table reference
     */
    public Tokens(String alias) {
        this(DSL.name(alias), TOKENS);
    }

    /**
     * Create an aliased <code>auth.tokens</code> table reference
     */
    public Tokens(Name alias) {
        this(alias, TOKENS);
    }

    private Tokens(Name alias, Table<TokensRecord> aliased) {
        this(alias, aliased, null);
    }

    private Tokens(Name alias, Table<TokensRecord> aliased, Field<?>[] parameters) {
        super(alias, null, aliased, parameters, DSL.comment(""));
    }

    public <O extends Record> Tokens(Table<O> child, ForeignKey<O, TokensRecord> key) {
        super(child, key, TOKENS);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Schema getSchema() {
        return Auth.AUTH;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Index> getIndexes() {
        return Arrays.<Index>asList(Indexes.TOKENS_FK_ISSUED_BY_USER, Indexes.TOKENS_FK_ISSUED_TO, Indexes.TOKENS_FK_TOKEN_TYPE_ID, Indexes.TOKENS_FK_UPDATED_BY_USER, Indexes.TOKENS_ID, Indexes.TOKENS_PRIMARY);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Identity<TokensRecord, Integer> getIdentity() {
        return Keys.IDENTITY_TOKENS;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public UniqueKey<TokensRecord> getPrimaryKey() {
        return Keys.KEY_TOKENS_PRIMARY;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<UniqueKey<TokensRecord>> getKeys() {
        return Arrays.<UniqueKey<TokensRecord>>asList(Keys.KEY_TOKENS_PRIMARY, Keys.KEY_TOKENS_ID);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<ForeignKey<TokensRecord, ?>> getReferences() {
        return Arrays.<ForeignKey<TokensRecord, ?>>asList(Keys.FK_ISSUED_TO, Keys.FK_TOKEN_TYPE_ID, Keys.FK_ISSUED_BY_USER, Keys.FK_UPDATED_BY_USER);
    }

    public Users fkIssuedTo() {
        return new Users(this, Keys.FK_ISSUED_TO);
    }

    public TokenTypes tokenTypes() {
        return new TokenTypes(this, Keys.FK_TOKEN_TYPE_ID);
    }

    public Users fkIssuedByUser() {
        return new Users(this, Keys.FK_ISSUED_BY_USER);
    }

    public Users fkUpdatedByUser() {
        return new Users(this, Keys.FK_UPDATED_BY_USER);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Tokens as(String alias) {
        return new Tokens(DSL.name(alias), this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Tokens as(Name alias) {
        return new Tokens(alias, this);
    }

    /**
     * Rename this table
     */
    @Override
    public Tokens rename(String name) {
        return new Tokens(DSL.name(name), null);
    }

    /**
     * Rename this table
     */
    @Override
    public Tokens rename(Name name) {
        return new Tokens(name, null);
    }
}