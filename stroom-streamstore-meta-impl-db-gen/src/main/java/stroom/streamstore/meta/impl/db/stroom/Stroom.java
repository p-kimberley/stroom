/*
 * This file is generated by jOOQ.
*/
package stroom.streamstore.meta.impl.db.stroom;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.annotation.Generated;

import org.jooq.Catalog;
import org.jooq.Table;
import org.jooq.impl.SchemaImpl;

import stroom.streamstore.meta.impl.db.DefaultCatalog;
import stroom.streamstore.meta.impl.db.stroom.tables.MetaKey;
import stroom.streamstore.meta.impl.db.stroom.tables.MetaNumericValue;
import stroom.streamstore.meta.impl.db.stroom.tables.StreamFeed;
import stroom.streamstore.meta.impl.db.stroom.tables.StreamProcessor;
import stroom.streamstore.meta.impl.db.stroom.tables.StreamType;
import stroom.streamstore.meta.impl.db.stroom.tables.Strm;


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
public class Stroom extends SchemaImpl {

    private static final long serialVersionUID = -319419674;

    /**
     * The reference instance of <code>stroom</code>
     */
    public static final Stroom STROOM = new Stroom();

    /**
     * The table <code>stroom.meta_key</code>.
     */
    public final MetaKey META_KEY = stroom.streamstore.meta.impl.db.stroom.tables.MetaKey.META_KEY;

    /**
     * The table <code>stroom.meta_numeric_value</code>.
     */
    public final MetaNumericValue META_NUMERIC_VALUE = stroom.streamstore.meta.impl.db.stroom.tables.MetaNumericValue.META_NUMERIC_VALUE;

    /**
     * The table <code>stroom.stream_feed</code>.
     */
    public final StreamFeed STREAM_FEED = stroom.streamstore.meta.impl.db.stroom.tables.StreamFeed.STREAM_FEED;

    /**
     * The table <code>stroom.stream_processor</code>.
     */
    public final StreamProcessor STREAM_PROCESSOR = stroom.streamstore.meta.impl.db.stroom.tables.StreamProcessor.STREAM_PROCESSOR;

    /**
     * The table <code>stroom.stream_type</code>.
     */
    public final StreamType STREAM_TYPE = stroom.streamstore.meta.impl.db.stroom.tables.StreamType.STREAM_TYPE;

    /**
     * The table <code>stroom.STRM</code>.
     */
    public final Strm STRM = stroom.streamstore.meta.impl.db.stroom.tables.Strm.STRM;

    /**
     * No further instances allowed
     */
    private Stroom() {
        super("stroom", null);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public Catalog getCatalog() {
        return DefaultCatalog.DEFAULT_CATALOG;
    }

    @Override
    public final List<Table<?>> getTables() {
        List result = new ArrayList();
        result.addAll(getTables0());
        return result;
    }

    private final List<Table<?>> getTables0() {
        return Arrays.<Table<?>>asList(
            MetaKey.META_KEY,
            MetaNumericValue.META_NUMERIC_VALUE,
            StreamFeed.STREAM_FEED,
            StreamProcessor.STREAM_PROCESSOR,
            StreamType.STREAM_TYPE,
            Strm.STRM);
    }
}
