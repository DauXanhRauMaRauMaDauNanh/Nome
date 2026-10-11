package io.netty.buffer;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.nio.charset.StandardCharsets;

/**
 * High performance, zero-garbage native off-heap ByteBuf.
 * Directly wraps native memory with Java 25 Foreign Memory MemorySegment.
 * Replaces heavy Netty buffer pooling, finalizers, and heap allocations.
 */
public class ByteBuf {

    private MemorySegment segment;
    private final Arena arena;
    private int readerIndex;
    private int writerIndex;
    private int refCount = 1;

    public ByteBuf(int initialCapacity) {
        this.arena = Arena.ofShared();
        this.segment = arena.allocate(initialCapacity);
        this.readerIndex = 0;
        this.writerIndex = 0;
    }

    public ByteBuf(MemorySegment segment) {
        this.arena = null;
        this.segment = segment;
        this.readerIndex = 0;
        this.writerIndex = (int) segment.byteSize();
    }

    public int capacity() {
        return (int) segment.byteSize();
    }

    public int readerIndex() {
        return readerIndex;
    }

    public ByteBuf readerIndex(int readerIndex) {
        this.readerIndex = readerIndex;
        return this;
    }

    public int writerIndex() {
        return writerIndex;
    }

    public ByteBuf writerIndex(int writerIndex) {
        this.writerIndex = writerIndex;
        return this;
    }

    public int readableBytes() {
        return writerIndex - readerIndex;
    }

    public int writableBytes() {
        return capacity() - writerIndex;
    }

    public boolean isReadable() {
        return readableBytes() > 0;
    }

    public boolean isWritable() {
        return writableBytes() > 0;
    }

    public byte readByte() {
        checkReadable(1);
        byte b = segment.get(ValueLayout.JAVA_BYTE, readerIndex);
        readerIndex++;
        return b;
    }

    public short readUnsignedByte() {
        return (short) (readByte() & 0xFF);
    }

    public ByteBuf writeByte(int value) {
        ensureWritable(1);
        segment.set(ValueLayout.JAVA_BYTE, writerIndex, (byte) value);
        writerIndex++;
        return this;
    }

    public short readShort() {
        checkReadable(2);
        short s = segment.get(ValueLayout.JAVA_SHORT, readerIndex);
        readerIndex += 2;
        return s;
    }

    public ByteBuf writeShort(int value) {
        ensureWritable(2);
        segment.set(ValueLayout.JAVA_SHORT, writerIndex, (short) value);
        writerIndex += 2;
        return this;
    }

    public int readInt() {
        checkReadable(4);
        int i = segment.get(ValueLayout.JAVA_INT, readerIndex);
        readerIndex += 4;
        return i;
    }

    public ByteBuf writeInt(int value) {
        ensureWritable(4);
        segment.set(ValueLayout.JAVA_INT, writerIndex, value);
        writerIndex += 4;
        return this;
    }

    public long readLong() {
        checkReadable(8);
        long l = segment.get(ValueLayout.JAVA_LONG, readerIndex);
        readerIndex += 8;
        return l;
    }

    public ByteBuf writeLong(long value) {
        ensureWritable(8);
        segment.set(ValueLayout.JAVA_LONG, writerIndex, value);
        writerIndex += 8;
        return this;
    }

    public float readFloat() {
        return Float.intBitsToFloat(readInt());
    }

    public ByteBuf writeFloat(float value) {
        return writeInt(Float.floatToIntBits(value));
    }

    public double readDouble() {
        return Double.longBitsToDouble(readLong());
    }

    public ByteBuf writeDouble(double value) {
        return writeLong(Double.doubleToLongBits(value));
    }

    public ByteBuf readBytes(byte[] dst) {
        return readBytes(dst, 0, dst.length);
    }

    public ByteBuf readBytes(byte[] dst, int dstIndex, int length) {
        checkReadable(length);
        MemorySegment.copy(segment, ValueLayout.JAVA_BYTE, readerIndex, dst, dstIndex, length);
        readerIndex += length;
        return this;
    }

    public ByteBuf writeBytes(byte[] src) {
        return writeBytes(src, 0, src.length);
    }

    public ByteBuf writeBytes(byte[] src, int srcIndex, int length) {
        ensureWritable(length);
        MemorySegment.copy(src, srcIndex, segment, ValueLayout.JAVA_BYTE, writerIndex, length);
        writerIndex += length;
        return this;
    }

    public ByteBuf clear() {
        readerIndex = 0;
        writerIndex = 0;
        return this;
    }

    public byte[] array() {
        byte[] arr = new byte[readableBytes()];
        MemorySegment.copy(segment, ValueLayout.JAVA_BYTE, readerIndex, arr, 0, arr.length);
        return arr;
    }

    public boolean hasArray() {
        return true;
    }

    public ByteBuf retain() {
        refCount++;
        return this;
    }

    public boolean release() {
        if (--refCount <= 0) {
            if (arena != null) {
                arena.close();
            }
            return true;
        }
        return false;
    }

    public MemorySegment memorySegment() {
        return segment;
    }

    private void checkReadable(int len) {
        if (readableBytes() < len) {
            throw new IndexOutOfBoundsException("Underflow: readable=" + readableBytes() + ", required=" + len);
        }
    }

    private void ensureWritable(int len) {
        if (writableBytes() < len) {
            int newCap = Math.max(capacity() * 2, capacity() + len);
            if (arena != null) {
                MemorySegment newSeg = arena.allocate(newCap);
                MemorySegment.copy(segment, 0, newSeg, 0, writerIndex);
                this.segment = newSeg;
            }
        }
    }
}
