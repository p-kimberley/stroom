package stroom.alert.impl;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.document.Document;
import stroom.alert.api.AlertProcessor;
import stroom.dashboard.expression.v1.FieldIndexMap;
import stroom.dashboard.impl.TableSettingsUtil;
import stroom.dictionary.api.WordListProvider;
import stroom.docref.DocRef;

import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.memory.MemoryIndex;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.TopDocs;

import stroom.index.impl.IndexStructure;
import stroom.index.impl.LuceneVersionUtil;
import stroom.index.impl.analyzer.AnalyzerFactory;
import stroom.index.shared.IndexField;
import stroom.index.shared.IndexFieldsMap;
import stroom.query.api.v2.ExpressionOperator;

import stroom.query.api.v2.Field;
import stroom.query.common.v2.CompiledFields;
import stroom.search.coprocessor.Error;
import stroom.search.coprocessor.Receiver;
import stroom.search.coprocessor.Values;
import stroom.search.extraction.ExtractionDecoratorFactory;
import stroom.search.extraction.ExtractionDecoratorFactory.AlertDefinition;
import stroom.search.impl.SearchException;
import stroom.search.impl.SearchExpressionQueryBuilder;
import stroom.util.logging.LambdaLogger;
import stroom.util.logging.LambdaLoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class AlertProcessorImpl implements AlertProcessor {
    private static final LambdaLogger LOGGER = LambdaLoggerFactory.getLogger(AlertProcessorImpl.class);

    private final AlertQueryHits alertQueryHits;

    private final WordListProvider wordListProvider;
    private final int maxBooleanClauseCount;
    private final IndexStructure indexStructure;
    //todo  work out how to use the timezone in the query
    private static final String DATE_TIME_LOCALE_SHOULD_BE_FROM_SEARCH = "UTC";

    private final List <RuleConfig> rules;

    private final Map<String, Analyzer> analyzerMap;

    private final ExtractionDecoratorFactory extractionDecoratorFactory;

    private Long currentStreamId = null;

    public AlertProcessorImpl (final ExtractionDecoratorFactory extractionDecoratorFactory,
                               final List<RuleConfig> rules,
                               final IndexStructure indexStructure,
                               final WordListProvider wordListProvider,
                               final int maxBooleanClauseCount){
        this.rules = rules;
        this.wordListProvider = wordListProvider;
        this.maxBooleanClauseCount = maxBooleanClauseCount;
        this.indexStructure = indexStructure;
        this.analyzerMap = new HashMap<>();
        if (indexStructure.getIndexFields() != null) {
            for (final IndexField indexField : indexStructure.getIndexFields()) {
                // Add the field analyser.
                final Analyzer analyzer = AnalyzerFactory.create(indexField.getAnalyzerType(),
                        indexField.isCaseSensitive());
                analyzerMap.put(indexField.getFieldName(), analyzer);
            }
        }
        alertQueryHits = new AlertQueryHits();
        this.extractionDecoratorFactory = extractionDecoratorFactory;
    }

    @Override
    public void addIfNeeded(final Document document) {

        Long streamId = findStreamId(document);
        if (streamId == null){
            LOGGER.warn("Unable to locate StreamId for document, alerting disabled for this stream");
            return;
        }
        if (currentStreamId == null){
            currentStreamId = streamId;
        }
        if (currentStreamId.longValue() != streamId.longValue()){
            throw new IllegalStateException("Unable to reuse AlertProcessorImpl for more than single stream" +
                    " was created with streamid " + currentStreamId +
                    " now applied to streamid " + streamId);
        }

        MemoryIndex memoryIndex = new MemoryIndex();
        if (analyzerMap == null || analyzerMap.size() == 0){
            //Don't create alerts if index isn't configured
            return;
        }

        Long eventId = findEventId (document);
        if (eventId == null) {
            LOGGER.warn("Unable to locate event id processing alerts for stream " + streamId);
            return;
        }

        for (IndexableField field : document){

            Analyzer fieldAnalyzer = analyzerMap.get(field.name());

            if (fieldAnalyzer != null){
                TokenStream tokenStream = field.tokenStream(fieldAnalyzer, null);
                if (tokenStream != null) {
                    memoryIndex.addField(field.name(),tokenStream, field.boost());
                }
            }
        }

        checkRules (eventId, memoryIndex);
    }


//    private Val[] createVals (final Document document){
//
//        //todo allow a different search extraction pipeline to be used.
//        //Currently the indexing pipeline must contain a superset of the values as the extraction pipeline
//        //This is possible when the same translation is used.
//
//        Val[] result = new Val[document.getFields().size()];
//        int fieldIndex = 0;
//        //See SearchResultOutputFilter for creation of Values
//        for (IndexableField field : document.getFields()){
//            if (field.numericValue() != null){
//                result[fieldIndex] = ValLong.create(field.numericValue().longValue());
//            } else {
//                result[fieldIndex] = ValString.create(field.stringValue());
//            }
//            fieldIndex++;
//        }
//        return result;
//    }

    private void checkRules (final long eventId, final MemoryIndex memoryIndex){
        if (rules == null) {
            return;
        }

        IndexSearcher indexSearcher = memoryIndex.createSearcher();
        final IndexFieldsMap indexFieldsMap = new IndexFieldsMap(indexStructure.getIndex().getFields());
        try {
            for (RuleConfig rule : rules) {
                if (matchQuery(indexSearcher, indexFieldsMap, rule.getExpression())){
                    //This query matches - now apply filters

                    //First get the original event XML

   //                 System.out.println ("Found a matching query rule");

                    alertQueryHits.addQueryHitForRule(rule,eventId);
                    LOGGER.debug("Adding {}:{} to rule {} from dashboard {}", currentStreamId, eventId, rule.getQueryId(), rule.getParams().get(AlertManagerImpl.DASHBOARD_NAME_KEY));
                } else {
                    LOGGER.trace("Not adding {}:{} to rule {} from dashboard {}", currentStreamId, eventId, rule.getQueryId(), rule.getParams().get(AlertManagerImpl.DASHBOARD_NAME_KEY));
                }
            }
        } catch (IOException ex){
            throw new RuntimeException("Unable to create alerts", ex);
        }
    }

    @Override
    public void createAlerts() {
        LOGGER.trace("Creating alerts...");

        int numTasks = 0;
        for (DocRef pipeline : alertQueryHits.getExtractionPipelines()) {
            LOGGER.trace("Iterating pipeline {}", pipeline.getName());
            Collection<RuleConfig> rulesForPipeline = alertQueryHits.getRulesForPipeline(pipeline);
            for (RuleConfig ruleConfig : rulesForPipeline){
                LOGGER.trace("--Iterating ruleConfig {}", ruleConfig.getQueryId());
                long[] eventIds = alertQueryHits.getSortedQueryHitsForRule(ruleConfig);
                if (eventIds != null && eventIds.length > 0) {

                    //todo - confirm that all tables share the same field map (and remove commented out code)
//                    for (TableSettings tableSettings : ruleConfig.getTableSettings()){
//                        LOGGER.trace("----Iterating tablesettings {}", tableSettings);
                        numTasks++;
//                        final Receiver receiver = new Receiver() {
//                            @Override
//                            public FieldIndexMap getFieldIndexMap() {
//                                return FieldIndexMap.forFields(ruleConfig.getAlertDefinitions().get(0).getTableComponentSettings()
//                                        .getFields().stream().map(t->t.getName())
//                                        .collect(Collectors.toList()).toArray(new String[0]));
//
//                                //The extraction pipeline's index map (wrong?)
////                                return FieldIndexMap.forFields(ruleConfig.getAlertDefinitions().get(0)
////                                        .getTableComponentSettings().getFields().stream().map(t->t.getName())
////                                        .collect(Collectors.toList()).toArray(new String[0]));
//                            }
//
//                            @Override
//                            public Consumer<Values> getValuesConsumer() {
//                                return values -> {};
//                            }
//
//                            @Override
//                            public Consumer<Error> getErrorConsumer() {
//                                return error -> LOGGER.error(error.getMessage(), error.getThrowable());
//                            }
//
//                            @Override
//                            public Consumer<Long> getCompletionCountConsumer() {
//                                return count -> {};
//                            }
//                        };

                    final Receiver receiver = new AlertProcessorReceiver(ruleConfig.getAlertDefinitions(),
                            ruleConfig.getParams());

                        extractionDecoratorFactory.createAlertExtractionTask(receiver, receiver,
                                currentStreamId, eventIds, pipeline,
                                ruleConfig.getAlertDefinitions(), ruleConfig.getParams());
                        LOGGER.trace("This AlertProcessorImpl has now created {} tasks during this call ", numTasks);
//                    }


                }
            }

        }
        alertQueryHits.clearHits();

    }

    private boolean matchQuery (final IndexSearcher indexSearcher, final IndexFieldsMap indexFieldsMap,
                                final ExpressionOperator query) throws IOException {

        try {

            final SearchExpressionQueryBuilder searchExpressionQueryBuilder = new SearchExpressionQueryBuilder(
                    wordListProvider, indexFieldsMap, maxBooleanClauseCount, DATE_TIME_LOCALE_SHOULD_BE_FROM_SEARCH, System.currentTimeMillis());
            final SearchExpressionQueryBuilder.SearchExpressionQuery luceneQuery = searchExpressionQueryBuilder
                    .buildQuery(LuceneVersionUtil.CURRENT_LUCENE_VERSION, query);

            TopDocs docs = indexSearcher.search(luceneQuery.getQuery(), 100);

            if (docs.totalHits == 0) {
                return false; //Doc does not match
            } else if (docs.totalHits == 1) {
                return true; //Doc matches
            } else {
                LOGGER.error("Unexpected number of documents {}  found by rule, should be 1 or 0.", docs.totalHits);
            }
        }
        catch (SearchException se){
            LOGGER.warn("Unable to create alerts for rule " + query + " due to " + se.getMessage());
        }
        return false;
    }

    private static Long findEventId(final Document document){
        try {
            return document.getField("EventId").numericValue().longValue();
        } catch (RuntimeException ex){
            return null;
        }
    }

    private static Long findStreamId(final Document document){
        try {
            return document.getField("StreamId").numericValue().longValue();
        } catch (RuntimeException ex){
            return null;
        }
    }

//
//    private String[] mapHit (final RuleConfig ruleConfig, final Document doc) {
//        final List<Field> fields = ruleConfig.getTableSettings().getFields();
//
//
//        FieldIndexMap fieldIndexMap = FieldIndexMap.forFields
//                (doc.getFields().stream().map(f -> f.name()).
//                        collect(Collectors.toList()).toArray(new String[doc.getFields().size()]));
//        final FieldFormatter fieldFormatter = new FieldFormatter(new FormatterFactory(DATE_TIME_LOCALE_SHOULD_BE_FROM_SEARCH));
//        //See CoprocessorsFactory for creation of field Index Map
//        final CompiledFields compiledFields = new CompiledFields(fields, fieldIndexMap, ruleConfig.getParamMap());
//
//
//        final String[] output = new String [ruleConfig.getTableSettings().getFields().size()];
//        int index = 0;
//        final Val[] inputVals = createVals (doc);
//        for (final CompiledField compiledField : compiledFields) {
//            final Expression expression = compiledField.getExpression();
//
//            if (expression != null) {
//                if (expression.hasAggregate()) {
//                    LOGGER.error("Aggregate functions not supported for dashboards in rules");
//                } else {
//                    final Generator generator = expression.createGenerator();
//
//                    generator.set(inputVals);
//                    Val value = generator.eval();
//                    output[index] = fieldFormatter.format(compiledField.getField(), value); //From TableResultCreator
//
//                    if (compiledField.getCompiledFilter() != null) {
//                        // If we are filtering then we need to evaluate this field
//                        // now so that we can filter the resultant value.
//
//                        if (compiledField.getCompiledFilter() != null && value != null && !compiledField.getCompiledFilter().match(value.toString())) {
//                            // We want to exclude this item.
//                            return null;
//                        }
//                    }
//                }
//            }
//
//            index++;
//        }
//
//        return output;
//    }

    private static class AlertProcessorReceiver implements Receiver {
        private final CompiledFields compiledFields;
        private final FieldIndexMap fieldIndexMap = new FieldIndexMap(true);

        AlertProcessorReceiver (final List<AlertDefinition> alertDefinitions,
                                final Map<String, String> paramMap){

            final List<Field> fields = TableSettingsUtil.mapFields(
                    alertDefinitions.stream().map(a -> a.getTableComponentSettings().getFields())
                    .reduce(new ArrayList<>(), (a,b)->{a.addAll(b); return a;}));

            compiledFields = new CompiledFields(fields, fieldIndexMap, paramMap);
        }

        @Override
        public FieldIndexMap getFieldIndexMap() {
            return fieldIndexMap;
        }

        @Override
        public Consumer<Values> getValuesConsumer() {
            return values -> {};
        }

        @Override
        public Consumer<Error> getErrorConsumer() {
            return error -> LOGGER.error(error.getMessage(), error.getThrowable());
        }

        @Override
        public Consumer<Long> getCompletionCountConsumer() {
            return count -> {};
        }
    };

}
