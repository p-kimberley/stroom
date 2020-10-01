package stroom.statistics.impl.sql.search;

import stroom.dashboard.expression.v1.Val;
import stroom.datasource.api.v2.AbstractField;
import stroom.statistics.impl.sql.shared.StatisticStoreDoc;
import stroom.task.api.TaskContext;

import io.reactivex.Flowable;

//TODO StatisticsDatabaseSearchService
public interface StatisticsSearchService {

    Flowable<Val[]> search(final TaskContext parentTaskContext,
                           final StatisticStoreDoc statisticStoreEntity,
                           final FindEventCriteria criteria,
                           final AbstractField[] fields);
}
