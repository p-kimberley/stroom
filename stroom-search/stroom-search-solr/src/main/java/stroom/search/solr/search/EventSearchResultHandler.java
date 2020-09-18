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

package stroom.search.solr.search;

import stroom.query.common.v2.Data;
import stroom.query.common.v2.EventRefs;
import stroom.query.common.v2.EventRefsPayload;
import stroom.query.common.v2.Payload;
import stroom.query.common.v2.ResultHandler;

import java.util.List;

class EventSearchResultHandler implements ResultHandler {
    private volatile EventRefs eventRefs;

    @Override
    public void handle(final List<Payload> payloads) {
        if (payloads != null) {
            for (final Payload payload : payloads) {
                if (payload instanceof EventRefsPayload) {
                    final EventRefsPayload eventRefsPayload = (EventRefsPayload) payload;
                    add(eventRefsPayload.getEventRefs());
                }
            }
        }
    }

    // Non private for testing purposes.
    private void add(final EventRefs eventRefs) {
        if (eventRefs != null && !Thread.currentThread().isInterrupted()) {
            if (this.eventRefs == null) {
                this.eventRefs = eventRefs;
            } else {
                this.eventRefs.add(eventRefs);
            }
        }
    }

    public EventRefs getEventRefs() {
        return eventRefs;
    }

    @Override
    public Data getResultStore(final String componentId) {
        return null;
    }
}
