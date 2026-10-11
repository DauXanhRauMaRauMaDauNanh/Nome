package it.unimi.dsi.fastutil.longs;

import java.util.PrimitiveIterator;

public interface LongIterator extends PrimitiveIterator.OfLong {
    long nextLong();

    @Override
    default Long next() {
        return nextLong();
    }
}
