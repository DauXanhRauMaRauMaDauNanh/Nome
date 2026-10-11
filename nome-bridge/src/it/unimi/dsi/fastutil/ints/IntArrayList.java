package it.unimi.dsi.fastutil.ints;

import java.util.*;

public class IntArrayList extends AbstractList<Integer> implements IntList, RandomAccess, Cloneable, java.io.Serializable {
    private int[] a;
    private int size;

    public IntArrayList(int capacity) {
        this.a = new int[Math.max(capacity, 10)];
        this.size = 0;
    }

    public IntArrayList() {
        this(10);
    }

    public IntArrayList(int[] elements) {
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
    public int getInt(int index) {
        if (index < 0 || index >= size) throw new IndexOutOfBoundsException();
        return a[index];
    }

    @Override
    public Integer get(int index) {
        return getInt(index);
    }

    @Override
    public boolean add(int k) {
        ensureCapacity(size + 1);
        a[size++] = k;
        return true;
    }

    @Override
    public void add(int index, int k) {
        if (index < 0 || index > size) throw new IndexOutOfBoundsException();
        ensureCapacity(size + 1);
        System.arraycopy(a, index, a, index + 1, size - index);
        a[index] = k;
        size++;
    }

    @Override
    public int set(int index, int k) {
        if (index < 0 || index >= size) throw new IndexOutOfBoundsException();
        int old = a[index];
        a[index] = k;
        return old;
    }

    @Override
    public int removeInt(int index) {
        if (index < 0 || index >= size) throw new IndexOutOfBoundsException();
        int old = a[index];
        int numMoved = size - index - 1;
        if (numMoved > 0) System.arraycopy(a, index + 1, a, index, numMoved);
        size--;
        return old;
    }

    @Override
    public boolean contains(int k) {
        for (int i = 0; i < size; i++) {
            if (a[i] == k) return true;
        }
        return false;
    }

    @Override
    public boolean rem(int k) {
        for (int i = 0; i < size; i++) {
            if (a[i] == k) {
                removeInt(i);
                return true;
            }
        }
        return false;
    }

    @Override
    public void clear() {
        size = 0;
    }

    public int[] toIntArray() {
        return Arrays.copyOf(a, size);
    }

    @Override
    public IntIterator iterator() {
        return new IntIterator() {
            private int pos = 0;
            public boolean hasNext() { return pos < size; }
            public int nextInt() {
                if (!hasNext()) throw new NoSuchElementException();
                return a[pos++];
            }
        };
    }
}
