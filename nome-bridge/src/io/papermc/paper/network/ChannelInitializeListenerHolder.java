package io.papermc.paper.network;

import net.kyori.adventure.key.Key;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class ChannelInitializeListenerHolder {
    private static final Map<Key, ChannelInitializeListener> listeners = new ConcurrentHashMap<>();

    private ChannelInitializeListenerHolder() {}

    public static void addListener(Key key, ChannelInitializeListener listener) {
        if (key != null && listener != null) {
            listeners.put(key, listener);
        }
    }

    public static void removeListener(Key key) {
        if (key != null) {
            listeners.remove(key);
        }
    }

    public static boolean hasListener(Key key) {
        return key != null && listeners.containsKey(key);
    }

    public static Map<Key, ChannelInitializeListener> getListeners() {
        return listeners;
    }
}
