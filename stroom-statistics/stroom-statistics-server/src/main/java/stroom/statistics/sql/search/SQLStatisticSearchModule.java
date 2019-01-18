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

package stroom.statistics.sql.search;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;
import stroom.entity.shared.Clearable;
import stroom.statistics.sql.StatisticsQueryService;
import stroom.util.HasHealthCheck;
import stroom.task.api.job.ScheduledJobsBinder;

public class SQLStatisticSearchModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(StatisticsQueryService.class).to(StatisticsQueryServiceImpl.class);
        bind(StatisticsSearchService.class).to(StatisticsSearchServiceImpl.class);

        final Multibinder<Clearable> clearableBinder = Multibinder.newSetBinder(binder(), Clearable.class);
        clearableBinder.addBinding().to(SqlStatisticsSearchResponseCreatorManager.class);

        final Multibinder<HasHealthCheck> hasHealthCheckBinder = Multibinder.newSetBinder(binder(), HasHealthCheck.class);
        hasHealthCheckBinder.addBinding().to(SqlStatisticsQueryResource.class);
    }
}