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

package stroom.test;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;
import stroom.entity.shared.Clearable;

public class DatabaseTestControlModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(CommonTestControl.class).to(DatabaseCommonTestControl.class);

        final Multibinder<Clearable> clearableBinder = Multibinder.newSetBinder(binder(), Clearable.class);
    }
    //    @Bean
//    public CommonTestScenarioCreator commonTestScenarioCreator(final FeedService feedService,
//                                                               final StreamStore streamStore,
//                                                               final StreamProcessorService streamProcessorService,
//                                                               final StreamProcessorFilterService streamProcessorFilterService,
//                                                               final IndexService indexService,
//                                                               final VolumeService volumeService,
//                                                               final NodeCache nodeCache) {
//        return new CommonTestScenarioCreator(feedService, streamStore, streamProcessorService, streamProcessorFilterService, indexService, volumeService, nodeCache);
//    }
//
//    @Bean
////    @Profile(StroomSpringProfiles.IT)
//    public CommonTranslationTest commonTranslationTest(final NodeCache nodeCache,
//                                                       final StreamTaskCreator streamTaskCreator,
//                                                       final StoreCreationTool storeCreationTool,
//                                                       final TaskManager taskManager,
//                                                       final StreamStore streamStore) {
//        return new CommonTranslationTest(nodeCache, streamTaskCreator, storeCreationTool, taskManager, streamStore);
//    }
//
//    @Bean
//    public ContentImportService contentImportService(final ImportExportService importExportService) {
//        return new ContentImportService(importExportService);
//    }
//
//    @Bean
//    public CommonTestControl commonTestControl(final VolumeService volumeService,
//                                               final ContentImportService contentImportService,
//                                               final StreamAttributeKeyService streamAttributeKeyService,
//                                               final IndexShardManager indexShardManager,
//                                               final IndexShardWriterCache indexShardWriterCache,
//                                               final DatabaseCommonTestControlTransactionHelper databaseCommonTestControlTransactionHelper,
//                                               final NodeConfig nodeConfig,
//                                               final StreamTaskCreator streamTaskCreator,
//                                               final StroomCacheManager stroomCacheManager,
//                                               final StroomBeanStore beanStore) {
//        return new DatabaseCommonTestControl(volumeService, contentImportService, streamAttributeKeyService, indexShardManager, indexShardWriterCache, databaseCommonTestControlTransactionHelper, nodeConfig, streamTaskCreator, stroomCacheManager, beanStore);
//    }
//
//    @Bean
//    public DatabaseCommonTestControlTransactionHelper databaseCommonTestControlTransactionHelper(final StroomEntityManager entityManager) {
//        return new DatabaseCommonTestControlTransactionHelper(entityManager);
//    }
}