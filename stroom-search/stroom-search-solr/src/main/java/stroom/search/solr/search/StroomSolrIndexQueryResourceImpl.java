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

package stroom.search.solr.search;

import stroom.datasource.api.v2.DataSource;
import stroom.docref.DocRef;
import stroom.event.logging.rs.api.AutoLogged;
import stroom.event.logging.rs.api.AutoLogged.OperationType;
import stroom.query.api.v2.FlatResult;
import stroom.query.api.v2.QueryKey;
import stroom.query.api.v2.SearchRequest;
import stroom.query.api.v2.SearchResponse;
import stroom.query.api.v2.TableResult;
import stroom.query.common.v2.SearchResponseCreator;
import stroom.query.common.v2.SearchResponseCreatorCache;
import stroom.search.solr.SolrIndexService;
import stroom.util.logging.LambdaLogger;
import stroom.util.logging.LambdaLoggerFactory;
import stroom.util.logging.LogUtil;

import com.codahale.metrics.annotation.Timed;

import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

@Singleton
@AutoLogged
public class StroomSolrIndexQueryResourceImpl implements StroomSolrIndexQueryResource {

    private static final LambdaLogger LOGGER = LambdaLoggerFactory.getLogger(StroomSolrIndexQueryResourceImpl.class);

    private final Provider<SolrSearchResponseCreatorManager> searchResponseCreatorManagerProvider;
    private final Provider<SolrIndexService> solrIndexServiceProvider;

    @Inject
    StroomSolrIndexQueryResourceImpl(final Provider<SolrSearchResponseCreatorManager>
                                             searchResponseCreatorManagerProvider,
                                     final Provider<SolrIndexService> solrIndexServiceProvider) {
        this.searchResponseCreatorManagerProvider = searchResponseCreatorManagerProvider;
        this.solrIndexServiceProvider = solrIndexServiceProvider;
    }

    @Timed
    @Override
    public DataSource getDataSource(final DocRef docRef) {
        return solrIndexServiceProvider.get().getDataSource(docRef);
    }

    @Timed
    @Override
    public SearchResponse search(final SearchRequest request) {
        //if this is the first call for this query key then it will create a searchResponseCreator (& store) that have
        //a lifespan beyond the scope of this request and then begin the search for the data
        //If it is not the first call for this query key then it will return the existing searchResponseCreator with
        //access to whatever data has been found so far
        final SearchResponseCreator searchResponseCreator = searchResponseCreatorManagerProvider.get()
                .get(new SearchResponseCreatorCache.Key(request));

        //create a response from the data found so far, this could be complete/incomplete
        SearchResponse searchResponse = searchResponseCreator.create(request);

        LOGGER.trace(() ->
                getResponseInfoForLogging(request, searchResponse));

        return searchResponse;
    }

    private String getResponseInfoForLogging(final SearchRequest request, final SearchResponse searchResponse) {
        String resultInfo;

        if (searchResponse.getResults() != null) {
            resultInfo = "\n" + searchResponse.getResults().stream()
                    .map(result -> {
                        if (result instanceof FlatResult) {
                            FlatResult flatResult = (FlatResult) result;
                            return LogUtil.message(
                                    "  FlatResult - componentId: {}, size: {}, ",
                                    flatResult.getComponentId(),
                                    flatResult.getSize());
                        } else if (result instanceof TableResult) {
                            TableResult tableResult = (TableResult) result;
                            return LogUtil.message(
                                    "  TableResult - componentId: {}, rows: {}, totalResults: {}, " +
                                            "resultRange: {}",
                                    tableResult.getComponentId(),
                                    tableResult.getRows().size(),
                                    tableResult.getTotalResults(),
                                    tableResult.getResultRange());
                        } else {
                            return "  Unknown type " + result.getClass().getName();
                        }
                    })
                    .collect(Collectors.joining("\n"));
        } else {
            resultInfo = "null";
        }

        return LogUtil.message("Return search response, key: {}, result sets: {}, " +
                        "complete: {}, errors: {}, results: {}",
                request.getKey().toString(),
                searchResponse.getResults(),
                searchResponse.complete(),
                searchResponse.getErrors(),
                resultInfo);
    }

    @Timed
    @Override
    @AutoLogged(OperationType.UNLOGGED)
    public Boolean destroy(final QueryKey queryKey) {
        searchResponseCreatorManagerProvider.get().remove(new SearchResponseCreatorCache.Key(queryKey));
        return Boolean.TRUE;
    }
}
