package it.unimi.dsi.fastutil.longs;

import java.util.Collection;

public interface LongCollection extends Collection<Long> {
    LongIterator iterator();
    boolean add(long key);
    boolean contains(long key);
    boolean rem(long key);
}
