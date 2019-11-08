/*
 * Copyright 2018 Crown Copyright
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

package stroom.search.resultstore;

import org.lmdbjava.Env;

import java.nio.ByteBuffer;
import java.nio.file.Path;

public class LmdbEnvironment {
    private final Env<ByteBuffer> lmdbEnv;
    private final Path dbDir;

    LmdbEnvironment(final Env<ByteBuffer> lmdbEnv, final Path dbDir) {
        this.lmdbEnv = lmdbEnv;
        this.dbDir = dbDir;
    }

    public Env<ByteBuffer> getLmdbEnv() {
        return lmdbEnv;
    }

    public Path getDbDir() {
        return dbDir;
    }
}
