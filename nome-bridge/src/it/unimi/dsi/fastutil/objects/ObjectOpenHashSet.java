package it.unimi.dsi.fastutil.objects;

import java.util.*;

public class ObjectOpenHashSet<K> extends HashSet<K> implements ObjectSet<K>, Cloneable, java.io.Serializable {
    public ObjectOpenHashSet(int expected) {
        super(Math.max(expected, 16));
    }

    public ObjectOpenHashSet() {
        super();
    }

    public ObjectOpenHashSet(Collection<? extends K> c) {
        super(c);
    }

    @SafeVarargs
    public static <K> ObjectOpenHashSet<K> of(K... elements) {
        ObjectOpenHashSet<K> set = new ObjectOpenHashSet<>(elements.length);
        Collections.addAll(set, elements);
        return set;
    }
}
