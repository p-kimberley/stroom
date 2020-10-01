/*
 * Copyright 2017 Crown Copyright
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package stroom.query.common.v2;

import stroom.dashboard.expression.v1.ExpressionParser;
import stroom.dashboard.expression.v1.FunctionFactory;
import stroom.dashboard.expression.v1.ParamFactory;
import stroom.datasource.api.v2.AbstractField;
import stroom.query.api.v2.ResultRequest;
import stroom.query.api.v2.SearchRequest;
import stroom.query.api.v2.TableSettings;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

public class CoprocessorSettingsFactory {
    public static String[] getAllRequiredFieldNames(final SearchRequest searchRequest) {
        final List<String> requiredFieldNames = new ArrayList<>();
        for (final ResultRequest resultRequest : searchRequest.getResultRequests()) {
            final TableSettings tableSettings = resultRequest.getMappings().get(0);
            if (tableSettings != null) {
                final String[] fieldNames = getRequiredFieldNames(tableSettings, Collections.emptyList());
                for (final String fieldName : fieldNames) {
                    if (!requiredFieldNames.contains(fieldName)) {
                        requiredFieldNames.add(fieldName);
                    }
                }
            }
        }
        return requiredFieldNames.toArray(new String[0]);
    }

    public static List<CoprocessorSettings> create(final SearchRequest searchRequest,
                                                   final List<AbstractField> availableFields,
                                                   final List<AbstractField> storedFields,
                                                   final List<String> mandatoryFieldNames) {
        // Group common settings.
        final Map<TableSettings, List<String>> groupMap = new HashMap<>();
        for (final ResultRequest resultRequest : searchRequest.getResultRequests()) {
            final String componentId = resultRequest.getComponentId();
            final TableSettings tableSettings = resultRequest.getMappings().get(0);
            if (tableSettings != null) {
                groupMap.computeIfAbsent(tableSettings, k -> new ArrayList<>()).add(componentId);
            }
        }
        final AtomicInteger atomicInteger = new AtomicInteger();
        return groupMap
                .entrySet()
                .stream()
                .map(e -> {
                    final String coprocessorId = String.valueOf(atomicInteger.incrementAndGet());
                    final String[] fieldNames = getRequiredFieldNames(e.getKey(), mandatoryFieldNames);
                    final AbstractField[] fields =
                            getRealFields(e.getKey(), availableFields, storedFields, fieldNames);
                    return new TableCoprocessorSettings(searchRequest.getKey(), coprocessorId, e.getValue(), e.getKey(), fieldNames, fields);
                })
                .collect(Collectors.toList());
    }

    private static AbstractField[] getRealFields(final TableSettings tableSettings,
                                                 final List<AbstractField> availableFields,
                                                 final List<AbstractField> storedFields,
                                                 final String[] requiredFieldNames) {
        final Map<String, AbstractField> availableFieldMap = availableFields
                .stream()
                .collect(Collectors.toMap(AbstractField::getName, Function.identity()));

        final Map<String, AbstractField> storedFieldMap = storedFields
                .stream()
                .collect(Collectors.toMap(AbstractField::getName, Function.identity()));

        final AbstractField[] fields = new AbstractField[requiredFieldNames.length];
        for (int i = 0; i < requiredFieldNames.length; i++) {
            final String name = requiredFieldNames[i];
            final AbstractField storedField = storedFieldMap.get(name);
            if (storedField != null) {
                fields[i] = storedField;
            } else if (tableSettings.extractValues() &&
                    tableSettings.getExtractionPipeline() != null &&
                    tableSettings.getExtractionPipeline().getUuid() != null) {
                // If we are doing extraction then we can use non stored fields.
                final AbstractField field = availableFieldMap.get(name);
                if (field != null) {
                    fields[i] = field;
                }
            }
        }

        return fields;
    }

    public static String[] getRequiredFieldNames(final TableSettings tableSettings,
                                                 final List<String> mandatoryFieldNames) {
        // Add mandatory field names to the field name list.
        final List<String> fieldNames = new ArrayList<>(mandatoryFieldNames);

        final ExpressionParser expressionParser = new ExpressionParser(new FunctionFactory(), new ParamFactory());
        tableSettings.getFields().forEach(field -> {
            try {
                final String[] referencedFieldNames = expressionParser.getReferencedFieldNames(field.getExpression());
                for (final String referencedFieldName : referencedFieldNames) {
                    if (!fieldNames.contains(referencedFieldName)) {
                        fieldNames.add(referencedFieldName);
                    }
                }
            } catch (final ParseException e) {
                throw new RuntimeException(e.getMessage(), e);
            }
        });

        return fieldNames.toArray(new String[0]);
    }
}
