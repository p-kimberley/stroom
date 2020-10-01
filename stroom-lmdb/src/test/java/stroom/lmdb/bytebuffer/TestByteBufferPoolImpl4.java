package stroom.lmdb.bytebuffer;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Map;

class TestByteBufferPoolImpl4 {

    private static final Logger LOGGER = LoggerFactory.getLogger(TestByteBufferPoolImpl4.class);

    @Test
    void testGetBuffer_defaultConfig() {
        doTest(new ByteBufferPoolConfig().getPooledByteBufferCounts(), 100, 1);
    }

    @Test
    void testGetBuffer_nullMap() {
        // No map means no pooled buffers so you get the size you asked for
        doTest(null, 50, 0);
    }

    @Test
    void testGetBuffer_emptyMap() {
        // No map means no pooled buffers so you get the size you asked for
        doTest(Collections.emptyMap(), 50, 0);
    }

    @Test
    void testGetBuffer_sparseMap() {
        // sparse map means buffer sizes between 1 and 10_000 will get the default count and thus
        // will be pooled
        doTest(Map.of(
                1, 20,
                10_000, 10), 100, 1);
    }

    @Test
    void testGetBuffer_zeroValue() {
        doTest(Map.of(
                1, 20,
                10, 10,
                100, 0), 50, 0);
    }

    private void doTest(final Map<Integer, Integer> pooledByteBufferCounts,
                        final int expectedBufferCapacity,
                        final int expectedPoolSize) {
        final ByteBufferPoolConfig config = new ByteBufferPoolConfig();
        config.setPooledByteBufferCounts(pooledByteBufferCounts);
        final ByteBufferPool byteBufferPool = new ByteBufferPoolImpl4(config);

        PooledByteBuffer pooledByteBuffer = byteBufferPool.getPooledByteBuffer(50);

        Assertions.assertThat(pooledByteBuffer.getByteBuffer().capacity())
                .isEqualTo(expectedBufferCapacity);

        pooledByteBuffer.close();

        Assertions.assertThat(byteBufferPool.getCurrentPoolSize())
                .isEqualTo(expectedPoolSize);

        LOGGER.info("System info: {}", byteBufferPool.getSystemInfo().getDetails());
    }
}