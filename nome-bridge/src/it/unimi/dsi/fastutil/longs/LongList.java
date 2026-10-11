package it.unimi.dsi.fastutil.longs;

import java.util.*;

public interface LongList extends List<Long>, LongCollection {
    long getLong(int index);
    long set(int index, long k);
    void add(int index, long k);
    long removeLong(int index);

    @Override
    default Long get(int index) {
        return getLong(index);
    }

    @Override
    default Long set(int index, Long element) {
        return set(index, element.longValue());
    }

    @Override
    default void add(int index, Long element) {
        add(index, element.longValue());
    }

    @Override
    default Long remove(int index) {
        return removeLong(index);
    }
}
