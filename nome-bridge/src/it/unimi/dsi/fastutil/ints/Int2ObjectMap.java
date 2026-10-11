package it.unimi.dsi.fastutil.ints;

import java.util.Map;

public interface Int2ObjectMap<V> extends Map<Integer, V> {
    V get(int key);
    V put(int key, V value);
    V remove(int key);
    boolean containsKey(int key);
    V defaultReturnValue();
    void defaultReturnValue(V rv);

    @Override
    default V get(Object key) {
        if (key instanceof Integer) return get(((Integer) key).intValue());
        return null;
    }

    @Override
    default V put(Integer key, V value) {
        return put(key.intValue(), value);
    }

    @Override
    default V remove(Object key) {
        if (key instanceof Integer) return remove(((Integer) key).intValue());
        return null;
    }

    @Override
    default boolean containsKey(Object key) {
        if (key instanceof Integer) return containsKey(((Integer) key).intValue());
        return false;
    }
}
