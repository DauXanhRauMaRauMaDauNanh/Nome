package io.netty.util;

import java.util.concurrent.ConcurrentHashMap;

public final class AttributeKey<T> {
    private static final ConcurrentHashMap<String, AttributeKey<?>> POOL = new ConcurrentHashMap<>();
    private final String name;

    private AttributeKey(String name) {
        this.name = name;
    }

    public String name() {
        return name;
    }

    @SuppressWarnings("unchecked")
    public static <T> AttributeKey<T> valueOf(String name) {
        return (AttributeKey<T>) POOL.computeIfAbsent(name, AttributeKey::new);
    }

    @SuppressWarnings("unchecked")
    public static <T> AttributeKey<T> newInstance(String name) {
        if (POOL.containsKey(name)) {
            throw new IllegalArgumentException("'" + name + "' is already in use");
        }
        AttributeKey<T> key = new AttributeKey<>(name);
        if (POOL.putIfAbsent(name, key) != null) {
            throw new IllegalArgumentException("'" + name + "' is already in use");
        }
        return key;
    }

    @Override
    public String toString() {
        return name;
    }
}
