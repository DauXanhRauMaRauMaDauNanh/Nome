package it.unimi.dsi.fastutil.longs;

import java.util.Set;

public interface LongSet extends Set<Long>, LongCollection {
    LongIterator iterator();
    boolean remove(long k);
}
