/*
 * Copyright 2016 Crown Copyright
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package stroom.search.impl.shard;

import stroom.query.common.v2.Receiver;

import org.apache.lucene.search.Query;
import org.apache.lucene.util.Version;

import java.util.concurrent.atomic.AtomicLong;

class IndexShardSearchTask {

    private final IndexShardQueryFactory queryFactory;
    private final long indexShardId;
    private final String[] storedFieldNames;
    private final Receiver receiver;
    private final AtomicLong hitCount;
    private int shardNumber;
    private int shardTotal;

    IndexShardSearchTask(final IndexShardQueryFactory queryFactory,
                         final long indexShardId,
                         final String[] storedFieldNames,
                         final Receiver receiver,
                         final AtomicLong hitCount) {
        this.queryFactory = queryFactory;
        this.indexShardId = indexShardId;
        this.storedFieldNames = storedFieldNames;
        this.receiver = receiver;
        this.hitCount = hitCount;
    }

    IndexShardQueryFactory getQueryFactory() {
        return queryFactory;
    }

    long getIndexShardId() {
        return indexShardId;
    }

    String[] getStoredFieldNames() {
        return storedFieldNames;
    }

    Receiver getReceiver() {
        return receiver;
    }

    AtomicLong getHitCount() {
        return hitCount;
    }

    int getShardNumber() {
        return shardNumber;
    }

    void setShardNumber(final int shardNumber) {
        this.shardNumber = shardNumber;
    }

    int getShardTotal() {
        return shardTotal;
    }

    void setShardTotal(final int shardTotal) {
        this.shardTotal = shardTotal;
    }

    interface IndexShardQueryFactory {

        Query getQuery(Version luceneVersion);
    }
}
