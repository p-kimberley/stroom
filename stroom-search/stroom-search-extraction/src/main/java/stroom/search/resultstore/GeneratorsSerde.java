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
import com.esotericsoftware.kryo.io.ByteBufferInputStream;
import com.esotericsoftware.kryo.io.ByteBufferOutputStream;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.pool.KryoFactory;
import com.esotericsoftware.kryo.pool.KryoPool;
import stroom.dashboard.expression.v1.Expression;
import stroom.dashboard.expression.v1.Generator;
import stroom.dashboard.expression.v1.ValSerialisers;
import stroom.lmdb.serde.AbstractKryoSerde;

import java.nio.ByteBuffer;
import java.util.function.Consumer;
import java.util.function.Function;

public class GeneratorsSerde extends AbstractKryoSerde<Generators> {
//    private volatile KryoPool kryoPool;
    private final Generators template;

    public GeneratorsSerde((final Expression[] expressions) {
        final Generator[] generators = new Generator[expressions.length];
        for (int i = 0; i < expressions.length; i++) {
            generators[i] = expressions[i].createGenerator();
        }
        template = new Generators(generators);
    }

    @Override
    public Generators read(final Input input) {
        kryoContext(kryo -> {
            for (final Generator generator : template.getGenerators()) {
                generator.read(kryo, input);
            }
        });
        return template;
    }

    @Override
    public void write(final Output output, final Generators generators) {
        kryoContext(kryo -> {
            for (final Generator generator : generators.getGenerators()) {
                generator.write(kryo, output);
            }
        });
    }
//
//    public void deserialize(final ByteBuffer byteBuffer, final Generators generators) {
//        try (final Input input = new Input(new ByteBufferInputStream(byteBuffer))) {
//            kryoContext(kryo -> {
//                for (final Generator generator : generators.getGenerators()) {
//                    generator.read(kryo, input);
//                }
//            });
//            byteBuffer.flip();
//        }
//    }
//
//    public void serialize(final ByteBuffer byteBuffer, final Generators generators) {
//        try (final Output output = new Output(new ByteBufferOutputStream(byteBuffer))) {
//            kryoContext(kryo -> {
//                for (final Generator generator : generators.getGenerators()) {
//                    generator.write(kryo, output);
//                }
//            });
//            output.flush();
//            byteBuffer.flip();
//        }
//    }
//
//    protected void kryoContext(final Consumer<Kryo> consumer) {
//        final KryoPool kryoPool = getPool();
//        final Kryo kryo = kryoPool.borrow();
//
//        try {
//            consumer.accept(kryo);
//        } finally {
//            kryoPool.release(kryo);
//        }
//    }
//
//    protected <R> R kryoContextResult(final Function<Kryo, R> function) {
//        final KryoPool kryoPool = getPool();
//        final Kryo kryo = kryoPool.borrow();
//
//        try {
//            return function.apply(kryo);
//        } finally {
//            kryoPool.release(kryo);
//        }
//    }
//
//    private KryoPool getPool() {
//        if (kryoPool == null) {
//            synchronized (this) {
//                if (kryoPool == null) {
//                    kryoPool = new KryoPool.Builder(this).build();
//                }
//            }
//        }
//        return kryoPool;
//    }
//
//    @Override
//    public Kryo create() {
//        final Kryo kryo = new Kryo();
//        ;
//        kryo.setRegistrationRequired(true);
//        ValSerialisers.register(kryo);
//        return kryo;
//    }


    @Override
    public Kryo create() {
        final Kryo kryo =  super.create();
        kryo.setRegistrationRequired(true);
        ValSerialisers.register(kryo);
        return kryo;
    }
}
