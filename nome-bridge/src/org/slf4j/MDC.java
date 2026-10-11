package org.slf4j;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Ultra-lean native MDC replacement.
 */
public final class MDC {
    private static final ThreadLocal<Map<String, String>> CONTEXT = ThreadLocal.withInitial(HashMap::new);

    private MDC() {}

    public static void put(String key, String val) {
        if (key != null) {
            CONTEXT.get().put(key, val);
        }
    }

    public static String get(String key) {
        if (key == null) return null;
        return CONTEXT.get().get(key);
    }

    public static void remove(String key) {
        if (key != null) {
            CONTEXT.get().remove(key);
        }
    }

    public static void clear() {
        CONTEXT.get().clear();
    }

    public static Map<String, String> getCopyOfContextMap() {
        return new HashMap<>(CONTEXT.get());
    }

    public static void setContextMap(Map<String, String> contextMap) {
        CONTEXT.get().clear();
        if (contextMap != null) {
            CONTEXT.get().putAll(contextMap);
        }
    }
}
