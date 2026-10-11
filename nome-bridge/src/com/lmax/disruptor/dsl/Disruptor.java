package com.lmax.disruptor.dsl;

import com.lmax.disruptor.*;
import java.util.concurrent.ThreadFactory;

public class Disruptor<T> {
    private final RingBuffer<T> ringBuffer;

    public Disruptor(EventFactory<T> eventFactory, int ringBufferSize, ThreadFactory threadFactory) {
        this.ringBuffer = RingBuffer.createMultiProducer(eventFactory, ringBufferSize);
    }

    @SafeVarargs
    public final void handleEventsWith(EventHandler<T>... handlers) {}

    public RingBuffer<T> start() {
        return ringBuffer;
    }

    public void shutdown() {}

    public RingBuffer<T> getRingBuffer() {
        return ringBuffer;
    }
}
