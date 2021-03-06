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

package stroom.pipeline.stepping;

import stroom.util.shared.TextRange;

/**
 * A recorder collects data from either the input or output of a pipeline
 * element.
 */
public interface Recorder {
    /**
     * Get the currently captured data from this recorder.
     *
     * @return Any data that has been captured by this recorder.
     */
    Object getData(TextRange textRange);

    /**
     * Clear the current data from this recorder.
     */
    void clear(TextRange textRange);
}
