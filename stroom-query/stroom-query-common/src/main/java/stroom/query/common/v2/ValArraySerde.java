package stroom.query.common.v2;

import stroom.dashboard.expression.v1.Type;
import stroom.dashboard.expression.v1.Val;
import stroom.dashboard.expression.v1.ValBoolean;
import stroom.dashboard.expression.v1.ValDouble;
import stroom.dashboard.expression.v1.ValErr;
import stroom.dashboard.expression.v1.ValInteger;
import stroom.dashboard.expression.v1.ValLong;
import stroom.dashboard.expression.v1.ValNull;
import stroom.dashboard.expression.v1.ValString;
import stroom.datasource.api.v2.AbstractField;
import stroom.datasource.api.v2.BooleanField;
import stroom.datasource.api.v2.DateField;
import stroom.datasource.api.v2.DocRefField;
import stroom.datasource.api.v2.DoubleField;
import stroom.datasource.api.v2.FloatField;
import stroom.datasource.api.v2.IdField;
import stroom.datasource.api.v2.IntegerField;
import stroom.datasource.api.v2.LongField;
import stroom.datasource.api.v2.TextField;
import stroom.lmdb.serde.AbstractKryoSerde;
import stroom.lmdb.serde.Serde;

import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

import java.util.function.BiConsumer;
import java.util.function.Function;

public class ValArraySerde extends AbstractKryoSerde<Val[]> implements Serde<Val[]> {
    private final Function<Input, Val>[] readers;
    private final BiConsumer<Output, Val>[] writers;

    public static ValArraySerde create(final AbstractField[] fields) {
        final Type[] valTypes = new Type[fields.length];
        for (int i = 0; i < fields.length; i++) {
            final AbstractField field = fields[i];

            if (field == null) {
                valTypes[i] = ValNull.TYPE;
            } else if (field instanceof IdField) {
                valTypes[i] = ValLong.TYPE;
            } else if (field instanceof BooleanField) {
                valTypes[i] = ValBoolean.TYPE;
            } else if (field instanceof IntegerField) {
                valTypes[i] = ValInteger.TYPE;
            } else if (field instanceof LongField) {
                valTypes[i] = ValLong.TYPE;
            } else if (field instanceof FloatField) {
                valTypes[i] = ValDouble.TYPE;
            } else if (field instanceof DoubleField) {
                valTypes[i] = ValDouble.TYPE;
            } else if (field instanceof DateField) {
                valTypes[i] = ValLong.TYPE;
            } else if (field instanceof TextField) {
                valTypes[i] = ValString.TYPE;
            } else if (field instanceof DocRefField) {
                valTypes[i] = ValNull.TYPE;
            }
        }
        return new ValArraySerde(valTypes);
    }

    @SuppressWarnings("unchecked")
    private ValArraySerde(Type[] valTypes) {
        this.readers = new Function[valTypes.length];
        this.writers = new BiConsumer[valTypes.length];

        for (int i = 0; i < valTypes.length; i++) {
            final Type type = valTypes[i];
            if (ValString.TYPE == type) {
                readers[i] = input -> ValString.create(input.readString());
                writers[i] = (output, val) -> output.writeString(val.toString());
            } else if (ValInteger.TYPE == type) {
                readers[i] = input -> ValInteger.create(input.readInt());
                writers[i] = (output, val) -> output.writeInt(val.toInteger());
            } else if (ValLong.TYPE == type) {
                readers[i] = input -> ValLong.create(input.readLong());
                writers[i] = (output, val) -> output.writeLong(val.toLong());
            } else if (ValDouble.TYPE == type) {
                readers[i] = input -> ValDouble.create(input.readDouble());
                writers[i] = (output, val) -> output.writeDouble(val.toDouble());
            } else if (ValBoolean.TYPE == type) {
                readers[i] = input -> ValBoolean.create(input.readBoolean());
                writers[i] = (output, val) -> output.writeBoolean(val.toBoolean());
            } else if (ValNull.TYPE == type) {
                readers[i] = input -> ValNull.INSTANCE;
                writers[i] = (output, val) -> {
                };
            } else if (ValErr.TYPE == type) {
                readers[i] = input -> ValErr.create(input.readString());
                writers[i] = (output, val) -> output.writeString(val.toString());
            } else {
                throw new RuntimeException("Unexpected type");
            }
        }
    }

//    @Override
//    public Val[] deserialize(final ByteBuffer byteBuffer) {
//        final Val[] vals = new Val[readers.length];
//        try (final Input input = new Input(new ByteBufferInputStream(byteBuffer))) {
//            for (int i = 0; i < readers.length; i++) {
//                vals[i] = readers[i].apply(input);
//            }
//        }
//        byteBuffer.flip();
//        return vals;
//    }
//
//    @Override
//    public void serialize(final ByteBuffer byteBuffer, final Val[] object) {
//        try (final Output output = new Output(new ByteBufferOutputStream(byteBuffer))) {
//            for (int i = 0; i < object.length; i++) {
//                writers[i].accept(output, object[i]);
//            }
//        }
//        byteBuffer.flip();
//    }
//
//

    @Override
    public void write(final Output output, final Val[] values) {
        for (int i = 0; i < values.length; i++) {
            writers[i].accept(output, values[i]);
        }
    }

    @Override
    public Val[] read(final Input input) {
        final Val[] vals = new Val[readers.length];
        for (int i = 0; i < readers.length; i++) {
            vals[i] = readers[i].apply(input);
        }
        return vals;
    }
}
