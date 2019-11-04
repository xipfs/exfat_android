package com.lenovo.exfat.core.util;

import java.nio.ByteBuffer;

public class ByteBufferUtils {

    public static void buffercopy(ByteBuffer src, int srcStart,
                                  ByteBuffer dest, int destStart, int len) {
        src.position(srcStart);
        src.limit(srcStart + len);

        dest.position(destStart);
        dest.limit(destStart + len);

        dest.put(src);
    }

    public static ByteArray toByteArray(ByteBuffer buf) {
        return new ByteArray(buf);
    }

    public static byte[] toArray(ByteBuffer buf) {
        byte[] array = new byte[buf.remaining()];
        buf.get(array);
        return array;
    }

    public static class ByteArray {
        private ByteBuffer buf;
        private int bufPosition;
        private int bufLimit;
        private byte[] array;

        private ByteArray(ByteBuffer buf) {
            this.buf = buf;
            this.bufPosition = buf.position();
            this.bufLimit = buf.limit();
            this.array = ByteBufferUtils.toArray(buf);
        }

        public byte[] toArray() {
            return array;
        }

        public void refreshByteBuffer() {
            buf.position(bufPosition);
            buf.limit(bufLimit);
            buf.put(array);
        }
    }
}
