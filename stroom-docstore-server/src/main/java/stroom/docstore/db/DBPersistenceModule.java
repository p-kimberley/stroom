/*
 * Copyright 2018 Crown Copyright
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

package stroom.docstore.db;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import stroom.docstore.Persistence;

import javax.sql.DataSource;

public class DBPersistenceModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(Persistence.class).to(DBPersistence.class).in(Singleton.class);
    }

//    @Bean
//    public DBPersistence dBPersistence(final DataSource dataSource) {
//        return new DBPersistence(dataSource);
//    }
}