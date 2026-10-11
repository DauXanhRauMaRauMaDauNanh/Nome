package it.unimi.dsi.fastutil.longs;

import java.util.*;

public class Long2ObjectOpenHashMap<V> extends AbstractMap<Long, V> implements Long2ObjectMap<V>, Cloneable, java.io.Serializable {
    private final Map<Long, V> map;
    private V defRetValue = null;

    public Long2ObjectOpenHashMap(int expected) {
        this.map = new HashMap<>(Math.max(expected, 16));
    }

    public Long2ObjectOpenHashMap() {
        this(16);
    }

    public Long2ObjectOpenHashMap(Map<? extends Long, ? extends V> m) {
        this.map = new HashMap<>(m);
    }

    @Override
    public V get(long key) {
        V val = map.get(key);
        return val != null ? val : defRetValue;
    }

    @Override
    public V put(long key, V value) {
        return map.put(key, value);
    }

    @Override
    public V remove(long key) {
        return map.remove(key);
    }

    @Override
    public boolean containsKey(long key) {
        return map.containsKey(key);
    }

    @Override
    public V defaultReturnValue() {
        return defRetValue;
    }

    @Override
    public void defaultReturnValue(V rv) {
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
    public Set<Entry<Long, V>> entrySet() {
        return map.entrySet();
    }
}
