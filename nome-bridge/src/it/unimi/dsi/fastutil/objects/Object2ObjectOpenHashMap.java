package it.unimi.dsi.fastutil.objects;

import java.util.*;

public class Object2ObjectOpenHashMap<K, V> extends HashMap<K, V> implements Object2ObjectMap<K, V>, Cloneable, java.io.Serializable {
    private V defRetValue = null;

    public Object2ObjectOpenHashMap(int expected) {
        super(Math.max(expected, 16));
    }

    public Object2ObjectOpenHashMap() {
        super(16);
    }

    public Object2ObjectOpenHashMap(Map<? extends K, ? extends V> m) {
        super(m);
    }

    @Override
    public V get(Object key) {
        V val = super.get(key);
        return val != null ? val : defRetValue;
    }

    @Override
    public V defaultReturnValue() {
        return defRetValue;
    }

    @Override
    public void defaultReturnValue(V rv) {
        this.defRetValue = rv;
    }
}
