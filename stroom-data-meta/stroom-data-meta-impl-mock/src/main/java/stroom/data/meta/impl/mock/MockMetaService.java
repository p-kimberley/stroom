package stroom.data.meta.impl.mock;

import stroom.data.meta.shared.AttributeMap;
import stroom.data.meta.shared.EffectiveMetaDataCriteria;
import stroom.data.meta.shared.FindMetaCriteria;
import stroom.data.meta.shared.Meta;
import stroom.data.meta.shared.MetaRow;
import stroom.data.meta.shared.MetaDataSource;
import stroom.data.meta.shared.MetaService;
import stroom.data.meta.shared.MetaProperties;
import stroom.data.meta.shared.Status;
import stroom.entity.shared.BaseResultList;
import stroom.entity.shared.Clearable;

import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Singleton
public class MockMetaService implements MetaService, Clearable {
    private final Set<String> feeds = new HashSet<>();
    private final Set<String> types = new HashSet<>();
    private final Map<Long, Meta> dataMap = new HashMap<>();

    /**
     * This id is used to emulate the primary key on the database.
     */
    private long currentId;

    @Override
    public Long getMaxId() {
        if (currentId == 0) {
            return null;
        }
        return currentId;
    }

    @Override
    public Meta create(final MetaProperties properties) {
        feeds.add(properties.getFeedName());
        types.add(properties.getTypeName());

        final MockMeta.Builder builder = new MockMeta.Builder();
        builder.parentDataId(properties.getParentId());
        builder.feedName(properties.getFeedName());
        builder.typeName(properties.getTypeName());
        builder.processorId(properties.getProcessorId());
        builder.processorTaskId(properties.getProcessorTaskId());
        builder.createMs(properties.getCreateMs());
        builder.effectiveMs(properties.getEffectiveMs());
        builder.statusMs(properties.getStatusMs());
        builder.status(Status.LOCKED);

        currentId++;
        builder.id(currentId);

        final Meta data = builder.build();
        dataMap.put(currentId, data);

        return data;
    }

    @Override
    public Meta getData(final long id) {
        return dataMap.get(id);
    }

    @Override
    public Meta getData(final long id, final boolean anyStatus) {
        return dataMap.get(id);
    }

    @Override
    public Meta updateStatus(final Meta data, final Status status) {
        Objects.requireNonNull(data, "Null data");

        final MockMeta result = (MockMeta) dataMap.get(data.getId());
        if (result != null) {
            result.status = status;
            result.statusMs = System.currentTimeMillis();
        }
        return result;
    }

    @Override
    public int updateStatus(final FindMetaCriteria criteria, final Status status) {
        return 0;
    }

    @Override
    public void addAttributes(final Meta data, final AttributeMap attributes) {
        // Do nothing.
    }

    @Override
    public int delete(final long id) {
        return delete(id, true);
    }

    @Override
    public int delete(final long id, final boolean lockCheck) {
        final Meta data = dataMap.get(id);
        if (lockCheck && !Status.UNLOCKED.equals(data.getStatus())) {
            return 0;
        }

        if (dataMap.remove(id) != null) {
            return 1;
        }
        return 0;
    }

    @Override
    public int getLockCount() {
        return (int) dataMap.values().stream().filter(data -> Status.LOCKED.equals(data.getStatus())).count();
    }

//    @Override
//    public Period getCreatePeriod() {
//        return new Period(0L, Long.MAX_VALUE);
//    }

    @Override
    public List<String> getFeeds() {
        return feeds.stream()
                .sorted(Comparator.naturalOrder())
                .collect(Collectors.toList());
    }

    @Override
    public List<String> getTypes() {
        return types.stream()
                .sorted(Comparator.naturalOrder())
                .collect(Collectors.toList());
    }

    @Override
    public BaseResultList<Meta> find(final FindMetaCriteria criteria) {
        final ExpressionMatcher expressionMatcher = new ExpressionMatcher(MetaDataSource.getExtendedFieldMap());
        final List<Meta> list = new ArrayList<>();
        for (final Entry<Long, Meta> entry : dataMap.entrySet()) {
            try {
                final Meta data = entry.getValue();
                final MetaRow row = new MetaRow(data);
                final Map<String, Object> attributeMap = AttributeMapUtil.createAttributeMap(row);
                if (expressionMatcher.match(attributeMap, criteria.getExpression())) {
                    list.add(data);
                }
            } catch (final RuntimeException e) {
                // Ignore.
            }
        }

        return BaseResultList.createUnboundedList(list);
    }

    @Override
    public BaseResultList<MetaRow> findRows(final FindMetaCriteria criteria) {
        return null;
    }

    @Override
    public List<MetaRow> findRelatedData(final long id, final boolean anyStatus) {
        return null;
    }

    @Override
    public Set<Meta> findEffectiveData(final EffectiveMetaDataCriteria criteria) {
        final Set<Meta> results = new HashSet<>();

        try {
            for (final Meta data : dataMap.values()) {
                boolean match = true;

                if (criteria.getType() != null && !criteria.getType().equals(data.getTypeName())) {
                    match = false;
                }
                if (criteria.getFeed() != null && !criteria.getFeed().equals(data.getFeedName())) {
                    match = false;
                }

                if (match) {
                    results.add(data);
                }
            }
        } catch (final RuntimeException e) {
            System.out.println(e.getMessage());
            // Ignore ... just a mock
        }

        return results;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        for (final long id : dataMap.keySet()) {
            final Meta data = dataMap.get(id);
            sb.append(data);
            sb.append("\n");
        }
        return sb.toString();
    }

    @Override
    public void clear() {
        feeds.clear();
        types.clear();
        dataMap.clear();
        currentId = 0;
    }

    public Map<Long, Meta> getDataMap() {
        return dataMap;
    }
}