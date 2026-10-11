package it.unimi.dsi.fastutil.ints;

import java.util.*;

public class IntOpenHashSet extends AbstractSet<Integer> implements IntSet, Cloneable, java.io.Serializable {
    private final Set<Integer> backing;

    public IntOpenHashSet(int expected) {
        this.backing = new HashSet<>(Math.max(expected, 16));
    }

    public IntOpenHashSet() {
        this(16);
    }

    public IntOpenHashSet(int[] a) {
        this(a.length);
        for (int v : a) add(v);
    }

    @Override
    public boolean add(int key) {
        return backing.add(key);
    }

    @Override
    public boolean add(Integer key) {
        return backing.add(key);
    }

    @Override
    public boolean contains(int key) {
        return backing.contains(key);
    }

    @Override
    public boolean rem(int key) {
        return backing.remove(key);
    }

    @Override
    public boolean remove(int k) {
        return backing.remove(k);
    }

    @Override
    public int size() {
        return backing.size();
    }

    @Override
    public boolean isEmpty() {
        return backing.isEmpty();
    }

    @Override
    public void clear() {
        backing.clear();
    }

    @Override
    public IntIterator iterator() {
        final Iterator<Integer> it = backing.iterator();
        return new IntIterator() {
            public boolean hasNext() { return it.hasNext(); }
            public int nextInt() { return it.next(); }
        };
    }
}
