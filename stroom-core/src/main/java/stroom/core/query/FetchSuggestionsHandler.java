/*
 * Copyright 2017 Crown Copyright
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
 *
 */

package stroom.core.query;

import stroom.query.shared.FetchSuggestionsAction;
import stroom.task.api.AbstractTaskHandler;
import stroom.util.shared.SharedList;
import stroom.util.shared.SharedString;

import javax.inject.Inject;
import java.util.List;
import java.util.stream.Collectors;


class FetchSuggestionsHandler extends AbstractTaskHandler<FetchSuggestionsAction, SharedList<SharedString>> {
    private final SuggestionsService suggestionsService;

    @Inject
    FetchSuggestionsHandler(final SuggestionsService suggestionsService) {
        this.suggestionsService = suggestionsService;
    }

    @Override
    public SharedList<SharedString> exec(final FetchSuggestionsAction task) {
        final List<String> list = suggestionsService.fetch(task.getDataSource(), task.getField(), task.getText());
        return list.stream()
                .map(SharedString::wrap)
                .collect(Collectors.toCollection(SharedList::new));
    }
}
