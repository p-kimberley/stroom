package stroom.pipeline.reader;

import stroom.bytebuffer.ByteBufferUtils;
import stroom.util.logging.LambdaLogger;
import stroom.util.logging.LambdaLoggerFactory;
import stroom.util.logging.LogUtil;

import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;
import java.util.Objects;
import java.util.function.Supplier;
import javax.annotation.concurrent.NotThreadSafe;

@NotThreadSafe
public class ByteStreamDecoder {

    private static final LambdaLogger LOGGER = LambdaLoggerFactory.getLogger(ByteStreamDecoder.class);

    private final CharsetDecoder charsetDecoder;
//    private final Supplier<Byte> byteSupplier;

    private static final int MAX_BYTES_PER_CHAR = 10;

    // Buffer used to feed the charsetDecoder, filled with one byte at a time till we have something
    // that will decode ok.
    private final java.nio.ByteBuffer inputBuffer = java.nio.ByteBuffer.allocate(MAX_BYTES_PER_CHAR);
    // The Buffer to output our decode char into, only needs to be length 1 as we are only dealing
    // in one char at time.
    private final java.nio.CharBuffer outputBuffer = java.nio.CharBuffer.allocate(2);

    /**
     * @param encoding The charset to use
     */
    public ByteStreamDecoder(final String encoding) {
        this(Objects.requireNonNull(Charset.forName(encoding)));
    }

    public ByteStreamDecoder(final Charset charset) {
        this.charsetDecoder = Objects.requireNonNull(charset)
                .newDecoder();
    }

    public DecodedChar decodeNextChar(final Supplier<Byte> byteSupplier) {
        // Clear the buffers ready for a new char's bytes
        inputBuffer.clear();
        outputBuffer.clear();
        outputBuffer.put((char) 0);
        outputBuffer.put((char) 0);
        outputBuffer.clear();

        DecodedChar result = null;
        boolean endOfSupply = false;

        // Start trying to decode a char from this position
//            int byteOffset = startOffset;

        boolean charDecoded = false;
        int loopCnt = 0;
        int byteCnt = 0;
        while (!charDecoded && loopCnt++ < MAX_BYTES_PER_CHAR) {
            byte b = 0;
            try {
                final Byte suppliedByte = byteSupplier.get();
                if (suppliedByte == null) {
                    // end of stream
                    endOfSupply = true;
                    break;
                }
                b = suppliedByte;
            } catch (Exception e) {
                throw new RuntimeException("Error getting next byte");
            }

            // Add the byte to our input buffer and get it ready for reading
            inputBuffer.put(b);
            inputBuffer.flip();

            // Attempt to decode the content of out input buffer
            final CoderResult coderResult = charsetDecoder.decode(
                    inputBuffer,
                    outputBuffer,
                    true);

            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("coderResult: {}, loopCnt: {}, inPos: {}, inLimit: {}, " +
                                "inBytes: [{}], outPos:{}, outLimit: {}, outputBuffer: [{}]",
                        coderResult,
                        loopCnt,
                        inputBuffer.position(),
                        inputBuffer.limit(),
                        ByteBufferUtils.byteBufferToHex(inputBuffer),
                        outputBuffer.position(),
                        outputBuffer.limit(),
                        outputBuffer.toString());
            }

            byteCnt++;

            // We may have only one byte of a multibyte char so a malformed result is likely
            // for any non ascii chars.
            if (!coderResult.isMalformed()) {
                // We have decoded something so output it
                charDecoded = true;
                final String decodedStr;
                if (outputBuffer.array()[0] != 0 && outputBuffer.array()[1] != 0) {
                    int codePoint = Character.toCodePoint(
                            outputBuffer.array()[0],
                            outputBuffer.array()[1]);
                    decodedStr = new String(new int[]{codePoint}, 0, 1);

                    LOGGER.trace("Multi-char character found with codePoint: [{}], decodedStr: [{}]",
                            codePoint, decodedStr);
                } else {
                    decodedStr = String.valueOf(outputBuffer.array()[0]);
                }

                LOGGER.trace("Decoded char {}, with byte count {}", decodedStr, byteCnt);
                result = new DecodedChar(decodedStr, byteCnt);
            } else {
                // Malformed so go round again as we obvs don't have enough bytes to form the char
                // Update the input buffer to take the next byte
                inputBuffer.limit(byteCnt + 1);
                inputBuffer.position(byteCnt);
//                    byteOffset++;
            }
        }
        if (!charDecoded && !endOfSupply) {
            throw new RuntimeException(LogUtil.message("Failed to decode char after {} iterations.", loopCnt));
        }

        return result;
    }

//    private boolean isByteOrderMark(final ByteBuffer byteBuffer) {
//
//    }

    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    public interface ByteSupplier {

        /**
         * @return The byte represented as an unsigned value 0-255 or -1 if there are
         * no more bytes to supply. This is equivalent to {@link InputStream#read()}.
         * <p>
         * Care need to be taken when comparing signed byte values to the result of this
         * method. Testing for -1 should be done before any kind of conversion/comparison
         * to another value.
         * <p>
         * e.g.
         * int b = supplyUnsignedByte();
         * if (b == -1) {
         * break;
         * }
         * if (arr[i] == (byte)
         */
        int supplyUnsignedByte();
    }


    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~


    /**
     * Holds a single 'character' (which may be represented as two char primitives)
     * along with the number of bytes used to represent that char.
     */
    public static class DecodedChar {

        private final String str;
        private final int byteCount;
        private static final char BYTE_ORDER_MARK = '\ufeff';

        public DecodedChar(final String str, final int byteCount) {
            this.str = str;
            this.byteCount = byteCount;
        }

        public String getAsString() {
            return str;
        }

        public int getByteCount() {
            return byteCount;
        }

        public int getCharCount() {
            return str.length();
        }

        public boolean isByteOrderMark() {
            return str.length() == 1 && str.charAt(0) == BYTE_ORDER_MARK;
        }

        public boolean isLineBreak() {
            return str.length() == 1 && str.charAt(0) == '\n';
        }

        public boolean isNonVisibleCharacter() {
            return str.codePoints()
                    .anyMatch(chr -> {
                        switch (Character.getType(chr)) {
                            case Character.CONTROL:
                            case Character.FORMAT:
                            case Character.PRIVATE_USE:
                            case Character.SURROGATE:
                            case Character.UNASSIGNED:
                                return true;
                            default:
                                return false;
                        }
                    });
        }

        @Override
        public String toString() {
            return "DecodedChar{" +
                    "str='" + str + '\'' +
                    ", byteCount=" + byteCount +
                    ", charCount=" + getCharCount() +
                    ", isLineBreak=" + isLineBreak() +
                    ", isNonVisibleCharacter=" + isNonVisibleCharacter() +
                    ", isByteOrderMark=" + isByteOrderMark() +
                    '}';
        }
    }
}
