package it.unimi.dsi.fastutil.ints;

import java.util.*;

public class Int2ObjectOpenHashMap<V> extends AbstractMap<Integer, V> implements Int2ObjectMap<V>, Cloneable, java.io.Serializable {
    private final Map<Integer, V> map;
    private V defRetValue = null;

    public Int2ObjectOpenHashMap(int expected) {
        this.map = new HashMap<>(Math.max(expected, 16));
    }

    public Int2ObjectOpenHashMap() {
        this(16);
    }

    public Int2ObjectOpenHashMap(Map<? extends Integer, ? extends V> m) {
        this.map = new HashMap<>(m);
    }

    @Override
    public V get(int key) {
        V val = map.get(key);
        return val != null ? val : defRetValue;
    }

    @Override
    public V put(int key, V value) {
        return map.put(key, value);
    }

    @Override
    public V remove(int key) {
        return map.remove(key);
    }

    @Override
    public boolean containsKey(int key) {
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
    public Set<Entry<Integer, V>> entrySet() {
        return map.entrySet();
    }
}
