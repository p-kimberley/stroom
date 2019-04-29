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

package stroom.dashboard.impl.visualisation;

import stroom.dashboard.shared.FetchVisualisationAction;
import stroom.task.api.AbstractTaskHandler;
import stroom.visualisation.shared.VisualisationDoc;

import javax.inject.Inject;

class FetchVisualisationHandler extends AbstractTaskHandler<FetchVisualisationAction, VisualisationDoc> {
    private final VisualisationService visualisationService;

    @Inject
    FetchVisualisationHandler(final VisualisationService visualisationService) {
        this.visualisationService = visualisationService;
    }

    @Override
    public VisualisationDoc exec(final FetchVisualisationAction action) {
        return visualisationService.fetch(action.getVisualisation());
    }
}
