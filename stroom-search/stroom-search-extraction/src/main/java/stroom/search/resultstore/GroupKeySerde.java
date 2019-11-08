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

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import stroom.dashboard.expression.v1.GroupKey;
import stroom.dashboard.expression.v1.ValSerialisers;
import stroom.lmdb.serde.AbstractKryoSerde;

public class GroupKeySerde extends AbstractKryoSerde<GroupKey> {
    @Override
    public GroupKey read(final Input input) {
        return kryoContextResult(kryo -> kryo.readObject(input, GroupKey.class));
    }

    @Override
    public void write(final Output output, final GroupKey object) {
        kryoContext(kryo -> kryo.writeObject(output, object));
    }

    @Override
    public Kryo create() {
        final Kryo kryo = super.create();
        kryo.setRegistrationRequired(true);
        ValSerialisers.register(kryo);
        return kryo;
    }
}
