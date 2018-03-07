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

package stroom.lifecycle;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import stroom.entity.StroomEntityManager;
import stroom.jobsystem.ScheduledTaskExecutor;
import stroom.properties.StroomPropertyService;
import stroom.security.SecurityContext;
import stroom.task.TaskHandler;
import stroom.task.TaskManager;
import stroom.util.spring.StroomBeanLifeCycle;
import stroom.util.spring.StroomScope;

public class LifecycleModule extends AbstractModule {
    @Override
    protected void configure() {
        final Multibinder<TaskHandler> taskHandlerBinder = Multibinder.newSetBinder(binder(), TaskHandler.class);
        taskHandlerBinder.addBinding().to(stroom.lifecycle.LifecycleTaskHandler.class);
    }
    //    @Bean
//    public LifecycleServiceImpl lifecycleService(final TaskManager taskManager,
//                                             final StroomBeanLifeCycle stroomBeanLifeCycle,
//                                             final StroomEntityManager entityManager,
//                                             final ScheduledTaskExecutor scheduledTaskExecutor,
//                                             final SecurityContext securityContext,
//                                                 final StroomPropertyService propertyService) {
//        return new LifecycleServiceImpl(taskManager, stroomBeanLifeCycle, entityManager, scheduledTaskExecutor, securityContext, propertyService);
//    }
//
//    @Bean
//    @Scope(value = StroomScope.TASK)
//    public LifecycleTaskHandler lifecycleTaskHandler() {
//        return new LifecycleTaskHandler();
//    }
}