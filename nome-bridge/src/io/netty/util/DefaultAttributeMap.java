package io.netty.util;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

public class DefaultAttributeMap implements AttributeMap {
    private final ConcurrentHashMap<AttributeKey<?>, Attribute<?>> map = new ConcurrentHashMap<>();

    @Override
    @SuppressWarnings("unchecked")
    public <T> Attribute<T> attr(AttributeKey<T> key) {
        return (Attribute<T>) map.computeIfAbsent(key, DefaultAttribute::new);
    }

    @Override
    public <T> boolean hasAttr(AttributeKey<T> key) {
        return map.containsKey(key);
    }

    private static class DefaultAttribute<T> implements Attribute<T> {
        private final AttributeKey<T> key;
        private final AtomicReference<T> value = new AtomicReference<>();

        DefaultAttribute(AttributeKey<T> key) { this.key = key; }

        public AttributeKey<T> key() { return key; }
        public T get() { return value.get(); }
        public void set(T val) { value.set(val); }
        public T getAndSet(T val) { return value.getAndSet(val); }
        public T setIfAbsent(T val) {
            value.compareAndSet(null, val);
            return value.get();
        }
        public T getAndRemove() { return value.getAndSet(null); }
        public boolean compareAndSet(T oldVal, T newVal) { return value.compareAndSet(oldVal, newVal); }
        public void remove() { value.set(null); }
    }
}
