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

package stroom.data.store.impl;

import stroom.meta.shared.FetchFullMetaInfoAction;
import stroom.meta.shared.FullMetaInfoResult;
import stroom.meta.shared.FullMetaInfoResult.Section;
import stroom.task.api.AbstractTaskHandler;

import javax.inject.Inject;
import java.util.List;

class FetchFullMetaInfoHandler extends AbstractTaskHandler<FetchFullMetaInfoAction, FullMetaInfoResult> {
    private final FullMetaInfoService fullMetaInfoService;

    @Inject
    FetchFullMetaInfoHandler(final FullMetaInfoService fullMetaInfoService) {
        this.fullMetaInfoService = fullMetaInfoService;
    }

    @Override
    public FullMetaInfoResult exec(final FetchFullMetaInfoAction action) {
        final List<Section> sections = fullMetaInfoService.fetch(action.getMeta());
        return new FullMetaInfoResult(sections);
    }
}
