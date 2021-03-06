package stroom.task.shared;

import stroom.util.shared.PageResponse;
import stroom.util.shared.ResultPage;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class TaskProgressResponse extends ResultPage<TaskProgress> {

    public TaskProgressResponse(final List<TaskProgress> values) {
        super(values);
    }

    @JsonCreator
    public TaskProgressResponse(@JsonProperty("values") final List<TaskProgress> values,
                                @JsonProperty("pageResponse") final PageResponse pageResponse) {
        super(values, pageResponse);
    }
}
