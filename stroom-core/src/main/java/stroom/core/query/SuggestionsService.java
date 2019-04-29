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

package stroom.core.query;

import stroom.datasource.api.v2.DataSourceField;
import stroom.docref.DocRef;
import stroom.meta.shared.MetaFieldNames;
import stroom.meta.shared.MetaService;
import stroom.meta.shared.Status;
import stroom.pipeline.PipelineStore;
import stroom.security.api.Security;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

class SuggestionsService {
    private final MetaService metaService;
    private final PipelineStore pipelineStore;
    private final Security security;

    @Inject
    SuggestionsService(final MetaService metaService,
                       final PipelineStore pipelineStore,
                       final Security security) {
        this.metaService = metaService;
        this.pipelineStore = pipelineStore;
        this.security = security;
    }

    List<String> fetch(final DocRef dataSource,
                       final DataSourceField field,
                       final String text) {
        return security.secureResult(() -> {
            if (dataSource != null) {
                if (MetaFieldNames.STREAM_STORE_DOC_REF.equals(dataSource)) {
                    if (field.getName().equals(MetaFieldNames.FEED_NAME)) {
                        return createFeedList(text);
                    }

                    if (field.getName().equals(MetaFieldNames.PIPELINE_UUID)) {
                        return pipelineStore.list().stream()
                                .filter(docRef -> docRef.getName().contains(text))
                                .map(DocRef::getName)
                                .sorted()
                                .collect(Collectors.toList());
                    }

                    if (field.getName().equals(MetaFieldNames.TYPE_NAME)) {
                        return createStreamTypeList(text);
                    }

                    if (field.getName().equals(MetaFieldNames.STATUS)) {
                        return Arrays.stream(Status.values())
                                .map(Status::getDisplayValue)
                                .sorted()
                                .collect(Collectors.toList());
                    }

//                    if (task.getField().getName().equals(StreamDataSource.NODE)) {
//                        return createList(nodeService, task.getText());
//                    }
                }
            }

            return Collections.emptyList();
        });
    }

//    @SuppressWarnings("unchecked")
//    private SharedList<SharedString> createList(final FindService service, final String text) {
//        final SharedList<SharedString> result = new SharedList<>();
//        final FindNamedEntityCriteria criteria = (FindNamedEntityCriteria) service.createCriteria();
//        criteria.setName(new StringCriteria(text, MatchStyle.WildEnd));
//        final List<Object> list = service.find(criteria);
//        list
//                .stream()
//                .sorted(Comparator.comparing(e -> ((NamedEntity) e).getName()))
//                .forEachOrdered(e -> result.add(SharedString.wrap(((NamedEntity) e).getName())));
//        return result;
//    }

    private List<String> createFeedList(final String text) {
        return metaService.getFeeds()
                .parallelStream()
                .filter(name -> name.startsWith(text))
                .sorted(Comparator.naturalOrder())
                .collect(Collectors.toList());
    }

    private List<String> createStreamTypeList(final String text) {
        return metaService.getTypes()
                .parallelStream()
                .filter(name -> name.startsWith(text))
                .sorted(Comparator.naturalOrder())
                .collect(Collectors.toList());
    }
}
