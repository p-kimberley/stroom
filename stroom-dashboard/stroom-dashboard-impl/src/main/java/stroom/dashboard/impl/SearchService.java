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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import stroom.dashboard.expression.v1.Expression;
import stroom.dashboard.expression.v1.ExpressionParser;
import stroom.dashboard.expression.v1.FieldIndexMap;
import stroom.dashboard.expression.v1.FunctionFactory;
import stroom.dashboard.expression.v1.ParamFactory;
import stroom.dashboard.impl.datasource.DataSourceProvider;
import stroom.dashboard.impl.datasource.DataSourceProviderRegistry;
import stroom.dashboard.impl.download.DelimitedTarget;
import stroom.dashboard.impl.download.ExcelTarget;
import stroom.dashboard.impl.download.SearchResultWriter;
import stroom.dashboard.impl.logging.SearchEventLog;
import stroom.dashboard.shared.ComponentResultRequest;
import stroom.dashboard.shared.DashboardQueryKey;
import stroom.dashboard.shared.DownloadSearchResultFileType;
import stroom.dashboard.shared.Field;
import stroom.dashboard.shared.Search;
import stroom.dashboard.shared.SearchRequest;
import stroom.dashboard.shared.SearchResponse;
import stroom.dashboard.shared.StoredQuery;
import stroom.dashboard.shared.TableResultRequest;
import stroom.dashboard.shared.ValidateExpressionResult;
import stroom.docref.DocRef;
import stroom.query.api.v2.Param;
import stroom.query.api.v2.Query;
import stroom.query.api.v2.Result;
import stroom.query.api.v2.ResultRequest;
import stroom.query.api.v2.Row;
import stroom.resource.api.ResourceStore;
import stroom.security.api.Security;
import stroom.security.shared.PermissionNames;
import stroom.security.shared.UserToken;
import stroom.storedquery.api.StoredQueryService;
import stroom.util.EntityServiceExceptionUtil;
import stroom.util.json.JsonUtil;
import stroom.util.shared.EntityServiceException;
import stroom.util.shared.ResourceGeneration;
import stroom.util.shared.ResourceKey;

import javax.inject.Inject;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;

@SuppressWarnings("unused")
class SearchService {
    private transient static final Logger LOGGER = LoggerFactory.getLogger(SearchService.class);

    private static final Pattern NON_BASIC_CHARS = Pattern.compile("[^A-Za-z0-9-_ ]");
    private static final Pattern MULTIPLE_SPACE = Pattern.compile(" +");

    private final SearchEventLog searchEventLog;
    private final ActiveQueriesManager activeQueriesManager;
    private final DataSourceProviderRegistry searchDataSourceProviderRegistry;
    private final SearchRequestMapper searchRequestMapper;
    private final ResourceStore resourceStore;
    private final StoredQueryService queryService;
    private final Security security;

    @Inject
    SearchService(final SearchEventLog searchEventLog,
                  final ActiveQueriesManager activeQueriesManager,
                  final DataSourceProviderRegistry searchDataSourceProviderRegistry,
                  final SearchRequestMapper searchRequestMapper,
                  final ResourceStore resourceStore,
                  final StoredQueryService queryService,
                  final Security security) {
        this.searchEventLog = searchEventLog;
        this.activeQueriesManager = activeQueriesManager;
        this.searchDataSourceProviderRegistry = searchDataSourceProviderRegistry;
        this.searchRequestMapper = searchRequestMapper;
        this.resourceStore = resourceStore;
        this.queryService = queryService;
        this.security = security;
    }

    ResourceGeneration downloadQuery(final DashboardQueryKey dashboardQueryKey,
                                     final SearchRequest searchRequest) {
        return security.secureResult(() -> {
            try {
                if (searchRequest == null) {
                    throw new EntityServiceException("Query is empty");
                }

                //API users will typically want all data so ensure Fetch.ALL is set regardless of what it was before
                if (searchRequest.getComponentResultRequests() != null) {
                    searchRequest.getComponentResultRequests()
                            .forEach((k, componentResultRequest) ->
                                    componentResultRequest.setFetch(ResultRequest.Fetch.ALL));
                }

                //convert our internal model to the model used by the api
                stroom.query.api.v2.SearchRequest apiSearchRequest = searchRequestMapper.mapRequest(
                        dashboardQueryKey,
                        searchRequest);

                if (apiSearchRequest == null) {
                    throw new EntityServiceException("Query could not be mapped to a SearchRequest");
                }

                //generate the export file
                String fileName = dashboardQueryKey.toString();
                fileName = NON_BASIC_CHARS.matcher(fileName).replaceAll("");
                fileName = MULTIPLE_SPACE.matcher(fileName).replaceAll(" ");
                fileName = fileName + ".json";

                final ResourceKey resourceKey = resourceStore.createTempFile(fileName);
                final Path outputFile = resourceStore.getTempFile(resourceKey);

                JsonUtil.writeValue(outputFile, apiSearchRequest);

                return new ResourceGeneration(resourceKey, new ArrayList<>());
            } catch (final RuntimeException e) {
                throw EntityServiceExceptionUtil.create(e);
            }
        });
    }


    ResourceGeneration downloadResults(final UserToken userToken,
                                       final String applicationInstanceId,
                                       final DashboardQueryKey queryKey,
                                       final SearchRequest searchRequest,
                                       final String componentId,
                                       final DownloadSearchResultFileType fileType,
                                       final boolean sample,
                                       final int percent,
                                       final String dateTimeLocale) {
        return security.secureResult(PermissionNames.DOWNLOAD_SEARCH_RESULTS_PERMISSION, () -> {
            ResourceKey resourceKey;

            final Search search = searchRequest.getSearch();
            final String searchSessionId = userToken + "_" + applicationInstanceId;
            final ActiveQueries activeQueries = activeQueriesManager.get(searchSessionId);

            // Make sure we have active queries for all current UI queries.
            // Note: This also ensures that the active query cache is kept alive
            // for all open UI components.
            final ActiveQuery activeQuery = activeQueries.getExistingQuery(queryKey);
            if (activeQuery == null) {
                throw new EntityServiceException("The requested search data is not available");
            }

            // Perform the search or update results.
            final DocRef dataSourceRef = search.getDataSourceRef();
            if (dataSourceRef == null || dataSourceRef.getUuid() == null) {
                throw new RuntimeException("No search data source has been specified");
            }

            // Get the data source provider for this query.
            final DataSourceProvider dataSourceProvider = searchDataSourceProviderRegistry
                    .getDataSourceProvider(dataSourceRef)
                    .orElseThrow(() ->
                            new RuntimeException("No search provider found for '" + dataSourceRef.getType() + "' data source"));

            stroom.query.api.v2.SearchRequest mappedRequest = searchRequestMapper.mapRequest(queryKey, searchRequest);
            stroom.query.api.v2.SearchResponse searchResponse = dataSourceProvider.search(mappedRequest);

            if (searchResponse == null || searchResponse.getResults() == null) {
                throw new EntityServiceException("No results can be found");
            }

            Result result = null;
            for (final Result res : searchResponse.getResults()) {
                if (res.getComponentId().equals(componentId)) {
                    result = res;
                    break;
                }
            }

            if (result == null) {
                throw new EntityServiceException("No result for component can be found");
            }

            if (!(result instanceof stroom.query.api.v2.TableResult)) {
                throw new EntityServiceException("Result is not a table");
            }

            final stroom.query.api.v2.TableResult tableResult = (stroom.query.api.v2.TableResult) result;

            // Import file.
            String fileName = queryKey.toString();
            fileName = NON_BASIC_CHARS.matcher(fileName).replaceAll("");
            fileName = MULTIPLE_SPACE.matcher(fileName).replaceAll(" ");
            fileName = fileName + "." + fileType.getExtension();

            resourceKey = resourceStore.createTempFile(fileName);
            final Path file = resourceStore.getTempFile(resourceKey);

            final ComponentResultRequest componentResultRequest = searchRequest.getComponentResultRequests().get(componentId);
            if (componentResultRequest == null) {
                throw new EntityServiceException("No component result request found");
            }

            if (!(componentResultRequest instanceof TableResultRequest)) {
                throw new EntityServiceException("Component result request is not a table");
            }

            final TableResultRequest tableResultRequest = (TableResultRequest) componentResultRequest;
            final List<Field> fields = tableResultRequest.getTableSettings().getFields();
            final List<stroom.query.api.v2.Row> rows = tableResult.getRows();

            download(fields, rows, file, fileType, sample, percent);

            return new ResourceGeneration(resourceKey, new ArrayList<>());
        });
    }

    private void download(final List<stroom.dashboard.shared.Field> fields, final List<Row> rows, final Path file,
                          final DownloadSearchResultFileType fileType, final boolean sample, final int percent) {
        try {
            final OutputStream outputStream = Files.newOutputStream(file);
            SearchResultWriter.Target target = null;

            // Write delimited file.
            switch (fileType) {
                case CSV:
                    target = new DelimitedTarget(outputStream, ",");
                    break;
                case TSV:
                    target = new DelimitedTarget(outputStream, "\t");
                    break;
                case EXCEL:
                    target = new ExcelTarget(outputStream);
                    break;
            }

            final SampleGenerator sampleGenerator = new SampleGenerator(sample, percent);
            final SearchResultWriter searchResultWriter = new SearchResultWriter(fields, rows, sampleGenerator);
            searchResultWriter.write(target);

        } catch (final IOException e) {
            throw EntityServiceExceptionUtil.create(e);
        }
    }

    Map<DashboardQueryKey, SearchResponse> poll(final UserToken userToken,
                                                final String applicationInstanceId,
                                                final Map<DashboardQueryKey, SearchRequest> searchActionMap) {
        return security.secureResult(() -> {
            // Elevate the users permissions for the duration of this task so they can read the index if they have 'use' permission.
            return security.useAsReadResult(() -> {
                if (LOGGER.isDebugEnabled()) {
                    final StringBuilder sb = new StringBuilder(
                            "Only the following search queries should be active for session '");
                    sb.append(userToken);
                    sb.append("'\n");
                    for (final DashboardQueryKey queryKey : searchActionMap.keySet()) {
                        sb.append("\t");
                        sb.append(queryKey.toString());
                    }
                    LOGGER.debug(sb.toString());
                }

                final String searchSessionId = userToken + "_" + applicationInstanceId;
                final ActiveQueries activeQueries = activeQueriesManager.get(searchSessionId);
                final Map<DashboardQueryKey, SearchResponse> searchResultMap = new HashMap<>();

//            // Fix query keys so they have session and user info.
//            for (final Entry<DashboardQueryKey, SearchRequest> entry : action.getSearchActionMap().entrySet()) {
//                final QueryKey queryKey = entry.getValues().getQueryKey();
//                queryKey.setSessionId(action.getSessionId());
//                queryKey.setUserId(action.getUserId());
//            }

                // Kill off any queries that are no longer required by the UI.
                activeQueries.destroyUnusedQueries(searchActionMap.keySet());

                // Get query results for every active query.
                for (final Entry<DashboardQueryKey, SearchRequest> entry : searchActionMap.entrySet()) {
                    final DashboardQueryKey queryKey = entry.getKey();

                    final SearchRequest searchRequest = entry.getValue();

                    if (searchRequest != null && searchRequest.getSearch() != null) {
                        final SearchResponse searchResponse = processRequest(activeQueries, queryKey, searchRequest);
                        if (searchResponse != null) {
                            searchResultMap.put(queryKey, searchResponse);
                        }
                    }
                }

                return searchResultMap;
            });
        });
    }

    private SearchResponse processRequest(final ActiveQueries activeQueries, final DashboardQueryKey queryKey, final SearchRequest searchRequest) {
        SearchResponse result;

        boolean newSearch = false;
        final Search search = searchRequest.getSearch();

        try {
            synchronized (SearchBusPollActionHandler.class) {
                // Make sure we have active queries for all current UI queries.
                // Note: This also ensures that the active query cache is kept alive
                // for all open UI components.
                final ActiveQuery activeQuery = activeQueries.getExistingQuery(queryKey);

                // If the query doesn't have an active query for this query key then
                // this is new.
                if (activeQuery == null) {
                    newSearch = true;

                    // Store the new active query for this query.
                    activeQueries.addNewQuery(queryKey, search.getDataSourceRef());

                    // Add this search to the history so the user can get back to this
                    // search again.
                    storeSearchHistory(queryKey, search);
                }
            }

            // Perform the search or update results.
            final DocRef dataSourceRef = search.getDataSourceRef();
            if (dataSourceRef == null || dataSourceRef.getUuid() == null) {
                throw new RuntimeException("No search data source has been specified");
            }

            // Get the data source provider for this query.
            final DataSourceProvider dataSourceProvider = searchDataSourceProviderRegistry
                    .getDataSourceProvider(dataSourceRef)
                    .orElseThrow(() ->
                            new RuntimeException("No search provider found for '" + dataSourceRef.getType() + "' data source"));

            stroom.query.api.v2.SearchRequest mappedRequest = searchRequestMapper.mapRequest(queryKey, searchRequest);
            stroom.query.api.v2.SearchResponse searchResponse = dataSourceProvider.search(mappedRequest);
            result = new SearchResponseMapper().mapResponse(searchResponse);

            if (newSearch) {
                // Log this search action for the current user.
                searchEventLog.search(search.getDataSourceRef(), search.getExpression(), search.getQueryInfo());
            }

        } catch (final RuntimeException e) {
            LOGGER.debug(e.getMessage(), e);

            if (newSearch) {
                searchEventLog.search(search.getDataSourceRef(), search.getExpression(), search.getQueryInfo(), e);
            }

            result = new SearchResponse();
            if (e.getMessage() == null) {
                result.setErrors(e.getClass().getName());
            } else {
                result.setErrors(e.getClass().getName() + ": " + e.getMessage());
            }
            result.setComplete(true);
        }

        return result;
    }

    private void storeSearchHistory(final DashboardQueryKey queryKey, final Search search) {
        // We only want to record search history for user initiated searches.
        if (search.isStoreHistory()) {
            try {
                // Add this search to the history so the user can get back to
                // this search again.
                List<Param> params;
                if (search.getParamMap() != null && search.getParamMap().size() > 0) {
                    params = new ArrayList<>(search.getParamMap().size());
                    for (final Entry<String, String> entry : search.getParamMap().entrySet()) {
                        params.add(new Param(entry.getKey(), entry.getValue()));
                    }
                } else {
                    params = null;
                }

                final Query query = new Query(search.getDataSourceRef(), search.getExpression(), params);

                final StoredQuery storedQuery = new StoredQuery();
                storedQuery.setName("History");
                storedQuery.setDashboardUuid(queryKey.getDashboardUuid());
                storedQuery.setComponentId(queryKey.getComponentId());
                storedQuery.setQuery(query);
                queryService.create(storedQuery);

            } catch (final RuntimeException e) {
                LOGGER.error(e.getMessage(), e);
            }
        }
    }

    ValidateExpressionResult validateExpression(final String expressionString) {
        try {
            final FieldIndexMap fieldIndexMap = new FieldIndexMap(true);
            final ExpressionParser expressionParser = new ExpressionParser(new FunctionFactory(), new ParamFactory());
            final Expression expression = expressionParser.parse(fieldIndexMap, expressionString);
            String correctedExpression = "";
            if (expression != null) {
                correctedExpression = expression.toString();
            }
            return new ValidateExpressionResult(true, correctedExpression);
        } catch (final ParseException e) {
            return new ValidateExpressionResult(false, e.getMessage());
        }
    }
}
