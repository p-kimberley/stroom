/*
 * This file is generated by jOOQ.
 */
package stroom.index.impl.db;


import javax.annotation.Generated;

import org.jooq.Index;
import org.jooq.OrderField;
import org.jooq.impl.Internal;

import stroom.index.impl.db.tables.IndexShard;
import stroom.index.impl.db.tables.IndexVolume;
import stroom.index.impl.db.tables.IndexVolumeGroup;
import stroom.index.impl.db.tables.IndexVolumeGroupLink;


/**
 * A class modelling indexes of tables of the <code>stroom</code> schema.
 */
@Generated(
    value = {
        "http://www.jooq.org",
        "jOOQ version:3.11.9"
    },
    comments = "This class is generated by jOOQ"
)
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class Indexes {

    // -------------------------------------------------------------------------
    // INDEX definitions
    // -------------------------------------------------------------------------

    public static final Index INDEX_SHARD_INDEX_SHARD_FK_VOLUME_ID = Indexes0.INDEX_SHARD_INDEX_SHARD_FK_VOLUME_ID;
    public static final Index INDEX_SHARD_INDEX_SHARD_INDEX_UUID = Indexes0.INDEX_SHARD_INDEX_SHARD_INDEX_UUID;
    public static final Index INDEX_SHARD_PRIMARY = Indexes0.INDEX_SHARD_PRIMARY;
    public static final Index INDEX_VOLUME_NODE_NAME_PATH = Indexes0.INDEX_VOLUME_NODE_NAME_PATH;
    public static final Index INDEX_VOLUME_PRIMARY = Indexes0.INDEX_VOLUME_PRIMARY;
    public static final Index INDEX_VOLUME_GROUP_INDEX_VOLUME_GROUP_NAME = Indexes0.INDEX_VOLUME_GROUP_INDEX_VOLUME_GROUP_NAME;
    public static final Index INDEX_VOLUME_GROUP_PRIMARY = Indexes0.INDEX_VOLUME_GROUP_PRIMARY;
    public static final Index INDEX_VOLUME_GROUP_LINK_INDEX_VOLUME_GROUP_LINK_FK_VOLUME_ID = Indexes0.INDEX_VOLUME_GROUP_LINK_INDEX_VOLUME_GROUP_LINK_FK_VOLUME_ID;
    public static final Index INDEX_VOLUME_GROUP_LINK_INDEX_VOLUME_GROUP_LINK_UNIQUE = Indexes0.INDEX_VOLUME_GROUP_LINK_INDEX_VOLUME_GROUP_LINK_UNIQUE;

    // -------------------------------------------------------------------------
    // [#1459] distribute members to avoid static initialisers > 64kb
    // -------------------------------------------------------------------------

    private static class Indexes0 {
        public static Index INDEX_SHARD_INDEX_SHARD_FK_VOLUME_ID = Internal.createIndex("index_shard_fk_volume_id", IndexShard.INDEX_SHARD, new OrderField[] { IndexShard.INDEX_SHARD.FK_VOLUME_ID }, false);
        public static Index INDEX_SHARD_INDEX_SHARD_INDEX_UUID = Internal.createIndex("index_shard_index_uuid", IndexShard.INDEX_SHARD, new OrderField[] { IndexShard.INDEX_SHARD.INDEX_UUID }, false);
        public static Index INDEX_SHARD_PRIMARY = Internal.createIndex("PRIMARY", IndexShard.INDEX_SHARD, new OrderField[] { IndexShard.INDEX_SHARD.ID }, true);
        public static Index INDEX_VOLUME_NODE_NAME_PATH = Internal.createIndex("node_name_path", IndexVolume.INDEX_VOLUME, new OrderField[] { IndexVolume.INDEX_VOLUME.NODE_NAME, IndexVolume.INDEX_VOLUME.PATH }, true);
        public static Index INDEX_VOLUME_PRIMARY = Internal.createIndex("PRIMARY", IndexVolume.INDEX_VOLUME, new OrderField[] { IndexVolume.INDEX_VOLUME.ID }, true);
        public static Index INDEX_VOLUME_GROUP_INDEX_VOLUME_GROUP_NAME = Internal.createIndex("index_volume_group_name", IndexVolumeGroup.INDEX_VOLUME_GROUP, new OrderField[] { IndexVolumeGroup.INDEX_VOLUME_GROUP.NAME }, true);
        public static Index INDEX_VOLUME_GROUP_PRIMARY = Internal.createIndex("PRIMARY", IndexVolumeGroup.INDEX_VOLUME_GROUP, new OrderField[] { IndexVolumeGroup.INDEX_VOLUME_GROUP.NAME }, true);
        public static Index INDEX_VOLUME_GROUP_LINK_INDEX_VOLUME_GROUP_LINK_FK_VOLUME_ID = Internal.createIndex("index_volume_group_link_fk_volume_id", IndexVolumeGroupLink.INDEX_VOLUME_GROUP_LINK, new OrderField[] { IndexVolumeGroupLink.INDEX_VOLUME_GROUP_LINK.FK_INDEX_VOLUME_ID }, false);
        public static Index INDEX_VOLUME_GROUP_LINK_INDEX_VOLUME_GROUP_LINK_UNIQUE = Internal.createIndex("index_volume_group_link_unique", IndexVolumeGroupLink.INDEX_VOLUME_GROUP_LINK, new OrderField[] { IndexVolumeGroupLink.INDEX_VOLUME_GROUP_LINK.FK_INDEX_VOLUME_GROUP_NAME, IndexVolumeGroupLink.INDEX_VOLUME_GROUP_LINK.FK_INDEX_VOLUME_ID }, true);
    }
}
