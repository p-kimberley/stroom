package stroom.search.resultstore;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import stroom.dashboard.expression.v1.Generator;
import stroom.dashboard.expression.v1.ValBoolean;

public class GeneratorsSerialiser extends Serializer<Generators> {
    @Override
    public void write(final Kryo kryo, final Output output, final Generators object) {
        final int size = object.getGenerators().length;
        output.writeInt(size, true);
        for (final Generator generator : object.getGenerators()) {
            generator.write(kryo, output);
        }
    }

    @Override
    public Generators read(final Kryo kryo, final Input input, final Class<Generators> type) {
        return ValBoolean.create(input.readBoolean());
    }
}
