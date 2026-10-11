package it.unimi.dsi.fastutil.ints;

import java.util.List;

public interface IntList extends List<Integer>, IntCollection {
    int getInt(int index);
    int set(int index, int k);
    void add(int index, int k);
    int removeInt(int index);

    @Override
    default Integer get(int index) {
        return getInt(index);
    }

    @Override
    default Integer set(int index, Integer element) {
        return set(index, element.intValue());
    }

    @Override
    default void add(int index, Integer element) {
        add(index, element.intValue());
    }

    @Override
    default Integer remove(int index) {
        return removeInt(index);
    }
}
