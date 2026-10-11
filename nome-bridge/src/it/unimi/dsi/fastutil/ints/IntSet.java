package it.unimi.dsi.fastutil.ints;

import java.util.Set;

public interface IntSet extends Set<Integer>, IntCollection {
    IntIterator iterator();
    boolean remove(int k);
}
