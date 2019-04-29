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

package stroom.task.impl;

import stroom.event.logging.api.HttpServletRequestHolder;
import stroom.task.api.AbstractTaskHandler;
import stroom.task.shared.FindUserTaskProgressAction;
import stroom.task.shared.TaskProgress;
import stroom.util.shared.ResultList;

import javax.inject.Inject;

class FindUserTaskProgressHandler extends AbstractTaskHandler<FindUserTaskProgressAction, ResultList<TaskProgress>> {
    private final TaskProgressService taskProgressService;
    private final transient HttpServletRequestHolder httpServletRequestHolder;

    @Inject
    FindUserTaskProgressHandler(final TaskProgressService taskProgressService,
                                final HttpServletRequestHolder httpServletRequestHolder) {
        this.taskProgressService = taskProgressService;
        this.httpServletRequestHolder = httpServletRequestHolder;
    }

    @Override
    public ResultList<TaskProgress> exec(final FindUserTaskProgressAction action) {
        return taskProgressService.findUserTaskProgress(action.getUserToken(), action.getTaskName(), getSessionId());
    }

    private String getSessionId() {
        if (httpServletRequestHolder == null) {
            return null;
        }
        return httpServletRequestHolder.getSessionId();
    }
}
