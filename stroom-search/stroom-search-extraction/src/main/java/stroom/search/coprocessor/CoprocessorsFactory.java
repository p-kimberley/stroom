package stroom.search.coprocessor;

import org.springframework.stereotype.Component;
import stroom.connectors.kafka.StroomKafkaProducerRecord;
import stroom.dashboard.expression.v1.FieldIndexMap;
import stroom.dashboard.expression.v1.Generator;
import stroom.dashboard.expression.v1.Key;
import stroom.hadoophdfsshaded.org.mortbay.util.SingletonList;
import stroom.mapreduce.v2.BlockingPairQueue;
import stroom.mapreduce.v2.OutputCollector;
import stroom.mapreduce.v2.PairQueue;
import stroom.mapreduce.v2.Reducer;
import stroom.mapreduce.v2.UnsafePairQueue;
import stroom.query.api.v2.Field;
import stroom.query.api.v2.Param;
import stroom.query.api.v2.TableSettings;
import stroom.query.common.v2.CompiledDepths;
import stroom.query.common.v2.CompiledFields;
import stroom.query.common.v2.Coprocessor;
import stroom.query.common.v2.CoprocessorSettings;
import stroom.query.common.v2.CoprocessorSettingsMap.CoprocessorKey;
import stroom.query.common.v2.GroupKey;
import stroom.query.common.v2.Item;
import stroom.query.common.v2.ItemMapper;
import stroom.query.common.v2.ItemReducer;
import stroom.query.common.v2.PayloadFactory;
import stroom.query.common.v2.TableCoprocessor;
import stroom.query.common.v2.TableCoprocessorSettings;
import stroom.query.common.v2.TablePayload;
import stroom.search.resultstore.GKey;
import stroom.search.resultstore.SearchResultStoreDb;
import stroom.search.resultstore.SearchResultStoreDbFactory;
import stroom.util.shared.HasTerminate;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Component
public class CoprocessorsFactory {
    private static final Key UNGROUPED = GKey.UNGROUPED;

    private final CoprocessorFactory coprocessorFactory;

    @Inject
    CoprocessorsFactory(final CoprocessorFactory coprocessorFactory) {
        this.coprocessorFactory = coprocessorFactory;
    }

    public Coprocessors create(final Map<CoprocessorKey, CoprocessorSettings> coprocessorSettingsMap, final String[] storedFields, final List<Param> params, final Consumer<Error> errorConsumer, final HasTerminate hasTerminate) {
            final Set<NewCoprocessor> coprocessors = new HashSet<>();
            final Map<String, FieldIndexMap> fieldIndexes = new HashMap<>();
            if (coprocessorSettingsMap != null) {
                // Get an array of stored index fields that will be used for getting stored data.
                final FieldIndexMap storedFieldIndexMap = new FieldIndexMap();
                for (final String storedField : storedFields) {
                    storedFieldIndexMap.create(storedField, true);
                }

                for (final Entry<CoprocessorKey, CoprocessorSettings> entry : coprocessorSettingsMap.entrySet()) {
                    final CoprocessorKey coprocessorKey = entry.getKey();
                    final CoprocessorSettings coprocessorSettings = entry.getValue();

                    // Figure out where the fields required by this coprocessor will be found.
                    FieldIndexMap fieldIndexMap = storedFieldIndexMap;
                    if (coprocessorSettings.extractValues() && coprocessorSettings.getExtractionPipeline() != null
                            && coprocessorSettings.getExtractionPipeline().getUuid() != null) {
                        fieldIndexMap = fieldIndexes.computeIfAbsent(coprocessorSettings.getExtractionPipeline().getUuid(), k -> new FieldIndexMap(true));
                    }

                    // Create a parameter map.
                    final Map<String, String> paramMap;
                    if (params != null) {
                        paramMap = params.stream()
                                .collect(Collectors.toMap(Param::getKey, Param::getValue));
                    } else {
                        paramMap = Collections.emptyMap();
                    }

                    Consumer<Values> valuesConsumer = null;
                    PayloadFactory payloadFactory = null;
                    if (coprocessorSettings instanceof TableCoprocessorSettings) {
                        final TableCoprocessorSettings tableCoprocessorSettings = (TableCoprocessorSettings) coprocessorSettings;

                        final TableSettings tableSettings = tableCoprocessorSettings.getTableSettings();
                        final List<Field> fields = tableSettings.getFields();
                        final CompiledFields compiledFields = new CompiledFields(fields, fieldIndexMap, paramMap);
                        final CompiledDepths compiledDepths = new CompiledDepths(fields, tableSettings.showDetail());

                        final Combiner combiner = new Combiner(compiledDepths.getDepths(), compiledDepths.getMaxDepth());
                        final ConcurrentHashMap<Key, Collection<Item>> map = new ConcurrentHashMap<>(10, 0.75F);
                        final OutputCollector<GroupKey, Item> storeCollector = (key, value) -> {

                            // The standard collect method is overridden so that items with a null
                            // key are passed straight to the output collector and will not undergo
                            // partitioning and reduction as we don't want to group items with null
                            // keys.
                            if (key != null && key.getValues() != null) {
                                map.compute(key, (k, v) -> {
                                    Collection<Item> values = v;
                                    if (values == null) {
                                        values = Collections.singletonList(value);
                                    } else {
                                        Item existingValue = values.iterator().next();
                                        existingValue = combiner.combine(existingValue, value);
                                        values = Collections.singletonList(existingValue);
                                    }
                                    return values;
                                });
                            } else {
                                map.compute(UNGROUPED, (k, v) -> {
                                    Collection<Item> values = v;
                                    if (values == null) {
                                        values = new ArrayList<>();
                                    }
                                    values.add(value);
                                    return values;
                                });
                            }

                        };

                        final ItemMapper mapper = new ItemMapper(storeCollector, compiledFields, compiledDepths.getMaxDepth(), compiledDepths.getMaxGroupDepth());
                        valuesConsumer = values -> mapper.collect(null, values.getValues());
                        payloadFactory = () -> {
                            final UnsafePairQueue<GroupKey, Item> outputQueue = new UnsafePairQueue<>();

                            map.keySet().forEach(key -> map.compute(key, (k, values) -> {
                                if (values != null) {
                                    if (key instanceof GroupKey) {
                                        final GroupKey groupKey = (GroupKey) key;
                                        values.forEach(v -> outputQueue.collect(groupKey, v));
                                    } else {
                                        values.forEach(v -> outputQueue.collect(null, v));
                                    }
                                }

                                return null;
                            }));

                            // Don't create a payload if the queue is empty.
                            if (outputQueue.size() == 0) {
                                return null;
                            }

                            return new TablePayload(outputQueue);
                        };

                    } else if (coprocessorSettings instanceof EventCoprocessorSettings) {
                        final EventCoprocessorSettings eventCoprocessorSettings = (EventCoprocessorSettings) coprocessorSettings;
                        final EventCoprocessor eventCoprocessor = new EventCoprocessor(eventCoprocessorSettings, fieldIndexMap);
                        valuesConsumer = values -> eventCoprocessor.receive(values.getValues());
                        payloadFactory = eventCoprocessor;
                    }

                    if (valuesConsumer != null && payloadFactory != null) {
                        final NewCoprocessor newCoprocessor = new NewCoprocessor(coprocessorKey, coprocessorSettings, fieldIndexMap, valuesConsumer, errorConsumer, payloadFactory);
                        coprocessors.add(newCoprocessor);
                    }
                }
            }

            return new Coprocessors(coprocessors);
    }
}
