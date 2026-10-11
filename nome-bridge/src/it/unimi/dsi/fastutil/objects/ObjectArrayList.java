package it.unimi.dsi.fastutil.objects;

import java.util.*;

public class ObjectArrayList<K> extends ArrayList<K> implements ObjectList<K>, RandomAccess, Cloneable, java.io.Serializable {
    public ObjectArrayList(int capacity) {
        super(capacity);
    }

    public ObjectArrayList() {
        super();
    }

    public ObjectArrayList(Collection<? extends K> c) {
        super(c);
    }

    @SafeVarargs
    public static <K> ObjectArrayList<K> of(K... elements) {
        ObjectArrayList<K> list = new ObjectArrayList<>(elements.length);
        Collections.addAll(list, elements);
        return list;
    }
}
