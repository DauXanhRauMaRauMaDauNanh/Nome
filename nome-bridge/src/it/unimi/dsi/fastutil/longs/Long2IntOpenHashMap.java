package it.unimi.dsi.fastutil.longs;

import java.util.*;

public class Long2IntOpenHashMap extends AbstractMap<Long, Integer> implements Long2IntMap, Cloneable, java.io.Serializable {
    private final Map<Long, Integer> map;
    private int defRetValue = 0;

    public Long2IntOpenHashMap(int expected) {
        this.map = new HashMap<>(Math.max(expected, 16));
    }

    public Long2IntOpenHashMap() {
        this(16);
    }

    public Long2IntOpenHashMap(Map<? extends Long, ? extends Integer> m) {
        this.map = new HashMap<>(m);
    }

    @Override
    public int get(long key) {
        Integer val = map.get(key);
        return val != null ? val : defRetValue;
    }

    @Override
    public int put(long key, int value) {
        Integer old = map.put(key, value);
        return old != null ? old : defRetValue;
    }

    @Override
    public int remove(long key) {
        Integer old = map.remove(key);
        return old != null ? old : defRetValue;
    }

    @Override
    public boolean containsKey(long key) {
        return map.containsKey(key);
    }

    @Override
    public int defaultReturnValue() {
        return defRetValue;
    }

    @Override
    public void defaultReturnValue(int rv) {
        this.defRetValue = rv;
    }

    @Override
    public int size() {
        return map.size();
    }

    @Override
    public boolean isEmpty() {
        return map.isEmpty();
    }

    @Override
    public void clear() {
        map.clear();
    }

    @Override
    public Set<Entry<Long, Integer>> entrySet() {
        return map.entrySet();
    }
}
