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

package stroom.event.logging.api;

import event.logging.Event;
import event.logging.EventDetail.Builder;
import event.logging.EventLoggingService;

import java.util.function.Consumer;

public interface StroomEventLoggingService extends EventLoggingService {

    Event createAction(final String typeId,
                       final String description);

    Event createAction(final String typeId,
                       final String description,
                       final Consumer<Builder<Void>> eventDetailBuilderConsumer);

    void log(final String typeId,
             final String description,
             final Consumer<Builder<Void>> eventDetailBuilderConsumer);
}
