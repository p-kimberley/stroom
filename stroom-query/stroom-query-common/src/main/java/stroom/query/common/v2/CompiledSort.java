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

import stroom.dashboard.expression.v1.Val;
import stroom.query.api.v2.Sort;
import stroom.query.api.v2.Sort.SortDirection;

import java.io.Serializable;
import java.util.Comparator;

public class CompiledSort implements Serializable {

    private static final long serialVersionUID = 719372020029496497L;

    private final int fieldIndex;
    private final int order;
    private final SortDirection direction;
    private final Comparator<Val> comparator;

    public CompiledSort(final int fieldIndex,
                        final Sort sort,
                        final Comparator<Val> comparator) {
        this.fieldIndex = fieldIndex;
        if (sort.getOrder() != null) {
            this.order = sort.getOrder();
        } else {
            this.order = 0;
        }
        this.direction = sort.getDirection();
        this.comparator = comparator;
    }

    public int getFieldIndex() {
        return fieldIndex;
    }

    public int getOrder() {
        return order;
    }

    public SortDirection getDirection() {
        return direction;
    }

    public Comparator<Val> getComparator() {
        return comparator;
    }

    @Override
    public String toString() {
        return "CompiledSort{" +
                "fieldIndex=" + fieldIndex +
                ", order=" + order +
                ", direction=" + direction +
                ", comparator=" + comparator +
                '}';
    }
}
