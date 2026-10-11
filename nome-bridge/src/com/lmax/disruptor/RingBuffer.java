package com.lmax.disruptor;

import java.util.concurrent.atomic.AtomicLong;

public class RingBuffer<E> {
    private final Object[] entries;
    private final int mask;
    private final AtomicLong cursor = new AtomicLong(-1);

    @SuppressWarnings("unchecked")
    public RingBuffer(EventFactory<E> eventFactory, int bufferSize) {
        int capacity = 1;
        while (capacity < bufferSize) capacity <<= 1;
        this.mask = capacity - 1;
        this.entries = new Object[capacity];
        for (int i = 0; i < capacity; i++) {
            entries[i] = eventFactory.newInstance();
        }
    }

    public static <E> RingBuffer<E> createSingleProducer(EventFactory<E> factory, int bufferSize) {
        return new RingBuffer<>(factory, bufferSize);
    }

    public static <E> RingBuffer<E> createMultiProducer(EventFactory<E> factory, int bufferSize) {
        return new RingBuffer<>(factory, bufferSize);
    }

    public long next() {
        return cursor.incrementAndGet();
    }

    @SuppressWarnings("unchecked")
    public E get(long sequence) {
        return (E) entries[(int) (sequence & mask)];
    }

    public void publish(long sequence) {}
}
