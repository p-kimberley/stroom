/*
 * Copyright 2017 Crown Copyright
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
 *
 */

package stroom.streamstore.meta.db;

import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import stroom.entity.shared.Clearable;

import javax.inject.Inject;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static stroom.streamstore.meta.impl.db.stroom.tables.StreamType.STREAM_TYPE;

class StreamTypeServiceImpl implements StreamTypeService, Clearable {
    private static final Logger LOGGER = LoggerFactory.getLogger(StreamTypeServiceImpl.class);

    private final Map<String, Integer> cache = new ConcurrentHashMap<>();

    private final DataSource dataSource;

    @Inject
    StreamTypeServiceImpl(final StreamMetaDataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public Integer getOrCreate(final String name) {
        Integer id = get(name);
        if (id == null) {
            // Try and create.
            id = create(name);
            if (id == null) {
                // Get again.
                id = get(name);
            }
        }

        return id;
    }

    @Override
    public List<String> list() {
        try (final Connection connection = dataSource.getConnection()) {
            final DSLContext context = DSL.using(connection, SQLDialect.MYSQL);
            return context
                    .select(STREAM_TYPE.NAME)
                    .from(STREAM_TYPE)
                    .fetch(STREAM_TYPE.NAME);

        } catch (final SQLException e) {
            LOGGER.error(e.getMessage(), e);
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    private Integer get(final String name) {
        Integer id = cache.get(name);
        if (id != null) {
            return id;
        }

        try (final Connection connection = dataSource.getConnection()) {
            final DSLContext context = DSL.using(connection, SQLDialect.MYSQL);
            id = context
                    .select(STREAM_TYPE.ID)
                    .from(STREAM_TYPE)
                    .where(STREAM_TYPE.NAME.eq(name))
                    .fetchOne(STREAM_TYPE.ID);

        } catch (final SQLException e) {
            LOGGER.error(e.getMessage(), e);
            throw new RuntimeException(e.getMessage(), e);
        }

        if (id != null) {
            cache.put(name, id);
        }

        return id;
    }

    private Integer create(final String name) {
        try (final Connection connection = dataSource.getConnection()) {
            final DSLContext context = DSL.using(connection, SQLDialect.MYSQL);
            final Integer id = context
                    .insertInto(STREAM_TYPE, STREAM_TYPE.NAME)
                    .values(name)
                    .returning(STREAM_TYPE.ID)
                    .fetchOne()
                    .getId();
            cache.put(name, id);
            return id;

        } catch (final SQLException | RuntimeException e) {
            // Expect errors in the case of duplicate names.
            LOGGER.debug(e.getMessage(), e);
        }

        return null;
    }

    @Override
    public void clear() {
        cache.clear();
    }

    int deleteAll() {
        try (final Connection connection = dataSource.getConnection()) {
            final DSLContext context = DSL.using(connection, SQLDialect.MYSQL);
            return context
                    .delete(STREAM_TYPE)
                    .execute();
        } catch (final SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }
}
