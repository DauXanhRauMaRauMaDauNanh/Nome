package it.unimi.dsi.fastutil.longs;

import java.util.*;

public class LongOpenHashSet extends AbstractSet<Long> implements LongSet, Cloneable, java.io.Serializable {
    private final Set<Long> backing;

    public LongOpenHashSet(int expected) {
        this.backing = new HashSet<>(Math.max(expected, 16));
    }

    public LongOpenHashSet() {
        this(16);
    }

    public LongOpenHashSet(long[] a) {
        this(a.length);
        for (long v : a) add(v);
    }

    @Override
    public boolean add(long key) {
        return backing.add(key);
    }

    @Override
    public boolean add(Long key) {
        return backing.add(key);
    }

    @Override
    public boolean contains(long key) {
        return backing.contains(key);
    }

    @Override
    public boolean rem(long key) {
        return backing.remove(key);
    }

    @Override
    public boolean remove(long k) {
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
    public LongIterator iterator() {
        final Iterator<Long> it = backing.iterator();
        return new LongIterator() {
            public boolean hasNext() { return it.hasNext(); }
            public long nextLong() { return it.next(); }
        };
    }
}
