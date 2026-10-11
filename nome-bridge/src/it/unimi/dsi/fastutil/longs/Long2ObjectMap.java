package it.unimi.dsi.fastutil.longs;

import java.util.Map;

public interface Long2ObjectMap<V> extends Map<Long, V> {
    V get(long key);
    V put(long key, V value);
    V remove(long key);
    boolean containsKey(long key);
    V defaultReturnValue();
    void defaultReturnValue(V rv);

    @Override
    default V get(Object key) {
        if (key instanceof Long) return get(((Long) key).longValue());
        return null;
    }

    @Override
    default V put(Long key, V value) {
        return put(key.longValue(), value);
    }

    @Override
    default V remove(Object key) {
        if (key instanceof Long) return remove(((Long) key).longValue());
        return null;
    }

    @Override
    default boolean containsKey(Object key) {
        if (key instanceof Long) return containsKey(((Long) key).longValue());
        return false;
    }
}
