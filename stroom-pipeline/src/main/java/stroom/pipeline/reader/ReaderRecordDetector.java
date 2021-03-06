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

package stroom.pipeline.reader;

import stroom.pipeline.errorhandler.ProcessException;
import stroom.pipeline.stepping.SteppingController;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FilterReader;
import java.io.IOException;
import java.io.Reader;

public class ReaderRecordDetector extends FilterReader {
    private static final Logger LOGGER = LoggerFactory.getLogger(ReaderRecordDetector.class);

    private static final int MAX_COUNT = 10000;
    private final char[] buffer = new char[1024];
    private final SteppingController controller;
    private long currentStepNo;
    private int offset;
    private int length;
    private boolean newStream = true;
    private boolean newRecord;
    private int count;
    private boolean end;

    ReaderRecordDetector(final Reader reader, final SteppingController controller) {
        super(reader);
        this.controller = controller;
    }

    @Override
    public int read(final char[] buf, final int off, final int len) throws IOException {
        if (controller == null) {
            return super.read(buf, off, len);
        }

        if (end) {
            return -1;
        }
        if (newStream) {
            currentStepNo = 0;
            controller.resetSourceLocation();

            newStream = false;
        }
        if (newRecord) {
            // Reset
            newRecord = false;
            count = 0;

            currentStepNo++;

            try {
                // Tell the controller that this is the end of a record.
                if (controller.endRecord(currentStepNo)) {
                    end = true;
                    return -1;
                }

                return 0;
            } catch (final ProcessException e) {
                throw e;
            } catch (final RuntimeException e) {
                LOGGER.error(e.getMessage(), e);
                throw e;
            }
        }

        if (length - offset == 0) {
            // Fill the buffer.
            length = super.read(buffer, 0, Math.min(buffer.length, len));
        }

        if (length == -1) {
            // The next time anybody tries to read from this reader it will be a
            // new stream.
            newStream = true;
            return -1;
        }

        int i = 0;
        while (i < length - offset) {
            final char c = buffer[offset + i];
            buf[off + i] = c;
            i++;
            count++;

            if (c == '\n' || count >= MAX_COUNT) {
                // The next time anybody tries to read from this reader it will
                // be a new record.
                newRecord = true;
                break;
            }
        }

        offset += i;

        return i;
    }
}
