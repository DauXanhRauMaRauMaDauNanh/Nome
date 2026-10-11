package it.unimi.dsi.fastutil.ints;

import java.util.Collection;

public interface IntCollection extends Collection<Integer> {
    IntIterator iterator();
    boolean add(int key);
    boolean contains(int key);
    boolean rem(int key);
}
