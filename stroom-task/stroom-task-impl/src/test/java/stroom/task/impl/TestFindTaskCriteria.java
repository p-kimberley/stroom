package stroom.task.impl;

import org.junit.jupiter.api.Test;
import stroom.task.shared.FindTaskCriteria;
import stroom.task.shared.TaskId;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

public class TestFindTaskCriteria {
    @Test
    void testUnrelatedTasks() {
        final TaskId task1 = TaskIdFactory.create();
        final TaskId task2 = TaskIdFactory.create();

        FindTaskCriteria criteria = new FindTaskCriteria(null, Set.of(task1), null);
        assertThat(criteria.isMatch(task1, null)).isTrue();
        assertThat(criteria.isMatch(task2, null)).isFalse();

        criteria = new FindTaskCriteria(null, Set.of(task1), Set.of(task1));
        assertThat(criteria.isMatch(task1, null)).isTrue();
        assertThat(criteria.isMatch(task2, null)).isFalse();

        criteria = new FindTaskCriteria(null, null, Set.of(task1));
        assertThat(criteria.isMatch(task1, null)).isTrue();
        assertThat(criteria.isMatch(task2, null)).isFalse();
    }

    @Test
    void testAncestorTasks1() {
        final TaskId task1 = TaskIdFactory.create();
        final TaskId task2 = TaskIdFactory.create(task1);
        final TaskId task3 = TaskIdFactory.create(task2);

        FindTaskCriteria criteria = new FindTaskCriteria(null, Set.of(task1), null);
        assertThat(criteria.isMatch(task1, null)).isTrue();
        assertThat(criteria.isMatch(task2, null)).isTrue();
        assertThat(criteria.isMatch(task3, null)).isTrue();

        criteria = new FindTaskCriteria(null, Set.of(task2), null);
        assertThat(criteria.isMatch(task1, null)).isFalse();
        assertThat(criteria.isMatch(task2, null)).isTrue();
        assertThat(criteria.isMatch(task3, null)).isTrue();

        criteria = new FindTaskCriteria(null, Set.of(task3), null);
        assertThat(criteria.isMatch(task1, null)).isFalse();
        assertThat(criteria.isMatch(task2, null)).isFalse();
        assertThat(criteria.isMatch(task3, null)).isTrue();
    }

    @Test
    void testAncestorTasks2() {
        final TaskId task1 = TaskIdFactory.create();
        final TaskId task2 = TaskIdFactory.create(task1);
        final TaskId task3 = TaskIdFactory.create(task2);

        FindTaskCriteria criteria = new FindTaskCriteria(null, null, Set.of(task1));
        assertThat(criteria.isMatch(task1, null)).isTrue();
        assertThat(criteria.isMatch(task2, null)).isFalse();
        assertThat(criteria.isMatch(task3, null)).isFalse();

        criteria = new FindTaskCriteria(null, null, Set.of(task2));
        assertThat(criteria.isMatch(task1, null)).isFalse();
        assertThat(criteria.isMatch(task2, null)).isTrue();
        assertThat(criteria.isMatch(task3, null)).isFalse();

        criteria = new FindTaskCriteria(null, null, Set.of(task3));
        assertThat(criteria.isMatch(task1, null)).isFalse();
        assertThat(criteria.isMatch(task2, null)).isFalse();
        assertThat(criteria.isMatch(task3, null)).isTrue();
    }
}
