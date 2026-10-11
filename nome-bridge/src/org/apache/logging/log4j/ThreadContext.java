package org.apache.logging.log4j;

import java.util.*;

public final class ThreadContext {
    public static final Map<String, String> EMPTY_MAP = Collections.emptyMap();
    private static final ThreadLocal<Map<String, String>> CONTEXT_MAP = ThreadLocal.withInitial(HashMap::new);

    public static void put(String key, String value) {
        CONTEXT_MAP.get().put(key, value);
    }

    public static String get(String key) {
        return CONTEXT_MAP.get().get(key);
    }

    public static void remove(String key) {
        CONTEXT_MAP.get().remove(key);
    }

    public static void clearMap() {
        CONTEXT_MAP.get().clear();
    }

    public static void clearAll() {
        clearMap();
    }

    public static Map<String, String> getImmutableContext() {
        return Collections.unmodifiableMap(new HashMap<>(CONTEXT_MAP.get()));
    }
}
