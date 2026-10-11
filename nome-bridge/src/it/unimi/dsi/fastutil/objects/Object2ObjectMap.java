package it.unimi.dsi.fastutil.objects;

import java.util.Map;

public interface Object2ObjectMap<K, V> extends Map<K, V> {
    V defaultReturnValue();
    void defaultReturnValue(V rv);
}
