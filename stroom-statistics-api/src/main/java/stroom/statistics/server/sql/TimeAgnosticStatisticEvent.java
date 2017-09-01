/*
 * Copyright 2016 Crown Copyright
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

package stroom.statistics.server.sql;

import stroom.statistics.shared.StatisticType;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TimeAgnosticStatisticEvent implements Serializable {
    private static final long serialVersionUID = 6308709037286356273L;
    private final String name;
    private final List<StatisticTag> tagList;
    private final Long count;
    private final Double value;
    private final StatisticType statisticType;
    // hold a cache of the positions of the tag names in the tagList so we can
    // quickly get them
    private final Map<String, Integer> tagPositionMap;
    private final int hashCode;

    private TimeAgnosticStatisticEvent(final String name, final List<StatisticTag> tagList, final Long count, final Double value, final StatisticType statisticType) {
        this.name = name;
        this.tagList = buildTagList(tagList);
        this.count = count;
        this.value = value;
        this.statisticType = statisticType;
        this.tagPositionMap = buildTagPositionMap();
        this.hashCode = buildHashCode();
    }

    /**
     * @param name
     * @param tagList Must be ordered by tag name. Can be null.
     * @param count
     */
    public static TimeAgnosticStatisticEvent createCount(final String name, final List<StatisticTag> tagList, final Long count) {
        if (count == null) {
            throw new IllegalArgumentException("Statistic must have a count");
        }
        return new TimeAgnosticStatisticEvent(name, tagList, count, null, StatisticType.COUNT);
    }

    /**
     * @param name
     * @param tagList Must be ordered by tag name. Can be null.
     * @param value
     */
    public static TimeAgnosticStatisticEvent createValue(final String name, final List<StatisticTag> tagList, final Double value) {
        if (value == null) {
            throw new IllegalArgumentException("Statistic must have a value");
        }
        return new TimeAgnosticStatisticEvent(name, tagList, null, value, StatisticType.VALUE);
    }

    private List<StatisticTag> buildTagList(final List<StatisticTag> tagList) {
        // build a new list object, sort it and make it unmodifiable
        // or just return an unmodifiable empty list
        if (tagList != null) {
            final List<StatisticTag> tempTagList = new ArrayList<>(tagList);

            return Collections.unmodifiableList(tempTagList);
        } else {
            return Collections.unmodifiableList(Collections.<StatisticTag>emptyList());
        }
    }

    private Map<String, Integer> buildTagPositionMap() {
        final Map<String, Integer> tagPositionMap = new HashMap<>();
        if (tagList != null) {
            int i = 0;
            for (final StatisticTag tag : tagList) {
                tagPositionMap.put(tag.getTag(), i++);
            }
        }
        return tagPositionMap;
    }

    public List<StatisticTag> getTagList() {
        return tagList;
    }

    public StatisticType getType() {
        if (value != null) {
            return StatisticType.VALUE;
        } else {
            return StatisticType.COUNT;
        }
    }

    public String getName() {
        return name;
    }

    public Double getValue() {
        return value;
    }

    public Long getCount() {
        return count;
    }

    public StatisticType getStatisticType() {
        return statisticType;
    }

    /**
     * @param tagName The name of the tag in a {@link StatisticTag} object
     * @return The position of the tag in the tag list (0 based)
     */
    public Integer getTagPosition(final String tagName) {
        return tagPositionMap.get(tagName);
    }

    @Override
    public String toString() {
        return "TimeAgnosticStatisticEvent [name=" + name + ", tagList=" + tagList + ", count=" + count + ", value="
                + value + ", statisticType=" + statisticType + "]";
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    private int buildHashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((count == null) ? 0 : count.hashCode());
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + ((statisticType == null) ? 0 : statisticType.hashCode());
        result = prime * result + ((tagList == null) ? 0 : tagList.hashCode());
        result = prime * result + ((value == null) ? 0 : value.hashCode());
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        final TimeAgnosticStatisticEvent other = (TimeAgnosticStatisticEvent) obj;
        if (count == null) {
            if (other.count != null)
                return false;
        } else if (!count.equals(other.count))
            return false;
        if (name == null) {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;
        if (statisticType != other.statisticType)
            return false;
        if (tagList == null) {
            if (other.tagList != null)
                return false;
        } else if (!tagList.equals(other.tagList))
            return false;
        if (value == null) {
            if (other.value != null)
                return false;
        } else if (!value.equals(other.value))
            return false;
        return true;
    }

}
