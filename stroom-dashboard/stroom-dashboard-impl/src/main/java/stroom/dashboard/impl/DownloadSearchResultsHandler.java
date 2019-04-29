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
 */

package stroom.dashboard.impl;

import stroom.dashboard.impl.logging.SearchEventLog;
import stroom.dashboard.shared.DownloadSearchResultsAction;
import stroom.dashboard.shared.Search;
import stroom.task.api.AbstractTaskHandler;
import stroom.util.EntityServiceExceptionUtil;
import stroom.util.shared.ResourceGeneration;

import javax.inject.Inject;

class DownloadSearchResultsHandler extends AbstractTaskHandler<DownloadSearchResultsAction, ResourceGeneration> {
    private final SearchService searchService;
    private final SearchEventLog searchEventLog;

    @Inject
    DownloadSearchResultsHandler(final SearchService searchService,
                                 final SearchEventLog searchEventLog) {
        this.searchService = searchService;
        this.searchEventLog = searchEventLog;
    }

    @Override
    public ResourceGeneration exec(final DownloadSearchResultsAction action) {
        ResourceGeneration resourceGeneration;
        final Search search = action.getSearchRequest().getSearch();

        try {
            resourceGeneration = searchService.downloadResults(
                    action.getUserToken(),
                    action.getApplicationInstanceId(),
                    action.getQueryKey(),
                    action.getSearchRequest(),
                    action.getComponentId(),
                    action.getFileType(),
                    action.isSample(),
                    action.getPercent(),
                    action.getDateTimeLocale());
            searchEventLog.downloadResults(search.getDataSourceRef(), search.getExpression(), search.getQueryInfo());
        } catch (final RuntimeException e) {
            searchEventLog.downloadResults(search.getDataSourceRef(), search.getExpression(), search.getQueryInfo(), e);
            throw EntityServiceExceptionUtil.create(e);
        }

        return resourceGeneration;
    }
}
