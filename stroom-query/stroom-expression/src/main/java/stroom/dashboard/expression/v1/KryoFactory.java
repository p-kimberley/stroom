package stroom.dashboard.expression.v1;

import com.esotericsoftware.kryo.Kryo;

public class KryoFactory {
    private static final Kryo KRYO;

    static {
        KRYO = new Kryo();
        KRYO.setRegistrationRequired(true);
        ValSerialisers.register(KRYO);
    }

    public Kryo get() {
        return KRYO;
    }
}
