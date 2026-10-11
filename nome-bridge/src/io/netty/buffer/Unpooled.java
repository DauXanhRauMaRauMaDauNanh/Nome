package io.netty.buffer;

import java.lang.foreign.MemorySegment;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public final class Unpooled {

    public static final ByteBuf EMPTY_BUFFER = new ByteBuf(0);

    private Unpooled() {}

    public static ByteBuf buffer() {
        return new ByteBuf(256);
    }

    public static ByteBuf buffer(int initialCapacity) {
        return new ByteBuf(initialCapacity);
    }

    public static ByteBuf wrappedBuffer(byte[] array) {
        if (array == null || array.length == 0) return EMPTY_BUFFER;
        ByteBuf buf = new ByteBuf(array.length);
        buf.writeBytes(array);
        return buf;
    }

    public static ByteBuf wrappedBuffer(MemorySegment segment) {
        return new ByteBuf(segment);
    }

    public static ByteBuf copiedBuffer(byte[] array) {
        return wrappedBuffer(array);
    }

    public static ByteBuf copiedBuffer(CharSequence string, Charset charset) {
        byte[] bytes = string.toString().getBytes(charset != null ? charset : StandardCharsets.UTF_8);
        return wrappedBuffer(bytes);
    }
}
