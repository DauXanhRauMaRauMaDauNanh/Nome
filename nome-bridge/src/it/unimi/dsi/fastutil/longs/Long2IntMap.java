package it.unimi.dsi.fastutil.longs;

import java.util.Map;

public interface Long2IntMap extends Map<Long, Integer> {
    int get(long key);
    int put(long key, int value);
    int remove(long key);
    boolean containsKey(long key);
    int defaultReturnValue();
    void defaultReturnValue(int rv);

    @Override
    default Integer get(Object key) {
        if (key instanceof Long) return get(((Long) key).longValue());
        return null;
    }

    @Override
    default Integer put(Long key, Integer value) {
        return put(key.longValue(), value != null ? value.intValue() : 0);
    }

    @Override
    default Integer remove(Object key) {
        if (key instanceof Long) return remove(((Long) key).longValue());
        return null;
    }

    @Override
    default boolean containsKey(Object key) {
        if (key instanceof Long) return containsKey(((Long) key).longValue());
        return false;
    }
}
