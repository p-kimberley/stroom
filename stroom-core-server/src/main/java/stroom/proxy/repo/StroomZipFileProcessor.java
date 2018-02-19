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

package stroom.proxy.repo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import stroom.util.io.StreamProgressMonitor;
import stroom.util.shared.ModelStringUtil;
import stroom.util.thread.BufferFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

public final class StroomZipFileProcessor {
    private static final Logger LOGGER = LoggerFactory.getLogger(StroomZipFileProcessor.class);

    public Long process(final Path file,
                        final StroomStreamHandler stroomStreamHandler,
                        final StreamProgressMonitor streamProgress,
                        final long startSequence) throws IOException {
        long entrySequence = startSequence;
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("process() - " + file);
        }

        try (final StroomZipFile stroomZipFile = new StroomZipFile(file)) {
            for (final String sourceName : stroomZipFile.getStroomZipNameSet().getBaseNameSet()) {
                final String targetName = StroomFileNameUtil.getIdPath(entrySequence++);

                sendEntry(stroomStreamHandler, stroomZipFile, sourceName, streamProgress,
                        new StroomZipEntry(null, targetName, StroomZipFileType.Meta));
                sendEntry(stroomStreamHandler, stroomZipFile, sourceName, streamProgress,
                        new StroomZipEntry(null, targetName, StroomZipFileType.Context));
                sendEntry(stroomStreamHandler, stroomZipFile, sourceName, streamProgress,
                        new StroomZipEntry(null, targetName, StroomZipFileType.Data));
            }
        }
        return entrySequence;
    }

    private void sendEntry(final StroomStreamHandler stroomStreamHandler, final StroomZipFile stroomZipFile,
                           final String sourceName, final StreamProgressMonitor streamProgress,
                           final StroomZipEntry targetEntry)
            throws IOException {
        final InputStream inputStream = stroomZipFile.getInputStream(sourceName, targetEntry.getStroomZipFileType());
        sendEntry(stroomStreamHandler, inputStream, streamProgress, targetEntry);
    }

    private void sendEntry(final StroomStreamHandler stroomStreamHandler, final InputStream inputStream,
                           final StreamProgressMonitor streamProgress, final StroomZipEntry targetEntry)
            throws IOException {
        if (inputStream != null) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("sendEntry() - " + targetEntry);
            }
            final byte[] buffer = BufferFactory.create();
            stroomStreamHandler.handleEntryStart(targetEntry);
            int read;
            long totalRead = 0;
            while ((read = inputStream.read(buffer)) != -1) {
                totalRead += read;
                streamProgress.progress(read);
                stroomStreamHandler.handleEntryData(buffer, 0, read);
            }
            stroomStreamHandler.handleEntryEnd();

            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("sendEntry() - " + targetEntry + " " + ModelStringUtil.formatIECByteSizeString(totalRead));
            }
            if (totalRead == 0) {
                LOGGER.warn("sendEntry() - " + targetEntry + " IS BLANK");
            }
            LOGGER.debug("sendEntry() - {} size is {}", targetEntry, totalRead);
        }
    }
}
