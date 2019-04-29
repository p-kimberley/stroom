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

package stroom.dashboard.impl.script;

import stroom.script.shared.FetchScriptAction;
import stroom.script.shared.ScriptDoc;
import stroom.task.api.AbstractTaskHandler;
import stroom.util.shared.SharedList;

import javax.inject.Inject;

class FetchScriptHandler extends AbstractTaskHandler<FetchScriptAction, SharedList<ScriptDoc>> {
    private final ScriptService scriptService;

    @Inject
    FetchScriptHandler(final ScriptService scriptService) {
        this.scriptService = scriptService;
    }

    @Override
    public SharedList<ScriptDoc> exec(final FetchScriptAction action) {
        return new SharedList<>(scriptService.fetch(action.getScript(), action.getLoadedScripts()));
    }
}
