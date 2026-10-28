/*
 * Copyright 2026 Crown Copyright
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

package stroom.search.elastic.search;

import stroom.query.api.datasource.QueryOptionSet;

import co.elastic.clients.elasticsearch._types.mapping.Property.Kind;

/// Maps Elasticsearch field mapping types to the field-level query options they support.
public final class ElasticQueryOptionSets {

    private ElasticQueryOptionSets() {
        // Utility class.
    }

    /// @param nativeType The Elasticsearch mapping type of a field, e.g. `keyword`.
    /// @return The query options the field supports, or null if it supports none.
    public static QueryOptionSet get(final String nativeType) {
        if (isTermLevelStringType(nativeType)) {
            return QueryOptionSet.ELASTIC_KEYWORD;
        } else if (Kind.Text.jsonValue().equals(nativeType) || Kind.MatchOnlyText.jsonValue().equals(nativeType)) {
            return QueryOptionSet.ELASTIC_TEXT;
        } else {
            return null;
        }
    }

    /// @param nativeType The Elasticsearch mapping type of a field.
    /// @return True if string values of the field are matched with term-level queries (`term`, `wildcard`,
    /// `regexp` and `fuzzy`) rather than full-text queries. This is the case for `keyword` and `wildcard`
    /// fields.
    public static boolean isTermLevelStringType(final String nativeType) {
        return Kind.Keyword.jsonValue().equals(nativeType) || Kind.Wildcard.jsonValue().equals(nativeType);
    }
}
