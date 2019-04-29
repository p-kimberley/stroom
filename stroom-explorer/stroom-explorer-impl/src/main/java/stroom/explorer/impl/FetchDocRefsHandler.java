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
 */

package stroom.explorer.impl;

import stroom.docref.DocRef;
import stroom.explorer.shared.FetchDocRefsAction;
import stroom.explorer.shared.SharedDocRef;
import stroom.task.api.AbstractTaskHandler;
import stroom.util.shared.SharedSet;

import javax.inject.Inject;
import java.util.Set;

class FetchDocRefsHandler
        extends AbstractTaskHandler<FetchDocRefsAction, SharedSet<SharedDocRef>> {
    private final ExplorerServiceImpl explorerService;
    private final ExplorerEventLog explorerEventLog;

    @Inject
    FetchDocRefsHandler(final ExplorerServiceImpl explorerService,
                        final ExplorerEventLog explorerEventLog) {
        this.explorerService = explorerService;
        this.explorerEventLog = explorerEventLog;
    }

    @Override
    public SharedSet<SharedDocRef> exec(final FetchDocRefsAction action) {
        SharedSet<SharedDocRef> result = new SharedSet<>();

        try {
            final Set<DocRef> docRefs = explorerService.fetchDocRefs(action.getDocRefs());
            docRefs.stream().map(SharedDocRef::create).forEach(result::add);
            explorerEventLog.fetchDocRefs(action.getDocRefs(), null);
        } catch (final RuntimeException e) {
            explorerEventLog.fetchDocRefs(action.getDocRefs(), e);
            throw e;
        }

        return result;
    }
}
