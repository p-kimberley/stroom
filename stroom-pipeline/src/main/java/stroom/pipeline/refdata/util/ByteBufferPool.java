package stroom.pipeline.refdata.util;

import stroom.util.shared.Clearable;
import stroom.util.sysinfo.HasSystemInfo;
import stroom.util.sysinfo.SystemInfoResult;

import java.nio.ByteBuffer;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

public interface ByteBufferPool extends Clearable, HasSystemInfo {

    PooledByteBuffer getPooledByteBuffer(int minCapacity);

    PooledByteBufferPair getPooledBufferPair(int minKeyCapacity, int minValueCapacity);

    <T> T getWithBuffer(int minCapacity, Function<ByteBuffer, T> work);

    void doWithBuffer(int minCapacity, Consumer<ByteBuffer> work);

    void doWithBufferPair(final int minKeyCapacity,
                          final int minValueCapacity,
                          final BiConsumer<ByteBuffer, ByteBuffer> work);

    /**
     * @return The number of buffers currently available in the pool and not on loan.
     */
    int getCurrentPoolSize();

    @Override
    void clear();

    @Override
    SystemInfoResult getSystemInfo();
}
