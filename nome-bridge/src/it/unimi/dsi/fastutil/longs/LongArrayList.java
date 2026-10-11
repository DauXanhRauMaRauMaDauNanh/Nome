package it.unimi.dsi.fastutil.longs;

import java.util.*;

public class LongArrayList extends AbstractList<Long> implements LongList, RandomAccess, Cloneable, java.io.Serializable {
    private long[] a;
    private int size;

    public LongArrayList(int capacity) {
        this.a = new long[Math.max(capacity, 10)];
        this.size = 0;
    }

    public LongArrayList() {
        this(10);
    }

    public LongArrayList(long[] elements) {
        this.a = elements.clone();
        this.size = elements.length;
    }

    private void ensureCapacity(int minCap) {
        if (minCap > a.length) {
            int newCap = Math.max(a.length * 2, minCap);
            a = Arrays.copyOf(a, newCap);
        }
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public boolean isEmpty() {
        return size == 0;
    }

    @Override
    public long getLong(int index) {
        if (index < 0 || index >= size) throw new IndexOutOfBoundsException();
        return a[index];
    }

    @Override
    public Long get(int index) {
        return getLong(index);
    }

    @Override
    public boolean add(long k) {
        ensureCapacity(size + 1);
        a[size++] = k;
        return true;
    }

    @Override
    public void add(int index, long k) {
        if (index < 0 || index > size) throw new IndexOutOfBoundsException();
        ensureCapacity(size + 1);
        System.arraycopy(a, index, a, index + 1, size - index);
        a[index] = k;
        size++;
    }

    @Override
    public long set(int index, long k) {
        if (index < 0 || index >= size) throw new IndexOutOfBoundsException();
        long old = a[index];
        a[index] = k;
        return old;
    }

    @Override
    public long removeLong(int index) {
        if (index < 0 || index >= size) throw new IndexOutOfBoundsException();
        long old = a[index];
        int numMoved = size - index - 1;
        if (numMoved > 0) System.arraycopy(a, index + 1, a, index, numMoved);
        size--;
        return old;
    }

    @Override
    public boolean contains(long k) {
        for (int i = 0; i < size; i++) {
            if (a[i] == k) return true;
        }
        return false;
    }

    @Override
    public boolean rem(long k) {
        for (int i = 0; i < size; i++) {
            if (a[i] == k) {
                removeLong(i);
                return true;
            }
        }
        return false;
    }

    @Override
    public void clear() {
        size = 0;
    }

    public long[] toLongArray() {
        return Arrays.copyOf(a, size);
    }

    @Override
    public LongIterator iterator() {
        return new LongIterator() {
            private int pos = 0;
            public boolean hasNext() { return pos < size; }
            public long nextLong() {
                if (!hasNext()) throw new NoSuchElementException();
                return a[pos++];
            }
        };
    }
}
