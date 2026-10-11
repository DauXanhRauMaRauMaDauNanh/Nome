package it.unimi.dsi.fastutil.ints;

import java.util.Iterator;
import java.util.PrimitiveIterator;
import java.util.function.IntConsumer;

public interface IntIterator extends PrimitiveIterator.OfInt {
    int nextInt();

    @Override
    default Integer next() {
        return nextInt();
    }
}
