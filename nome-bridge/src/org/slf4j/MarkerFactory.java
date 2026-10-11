package org.slf4j;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.Iterator;
import java.util.Collections;

public final class MarkerFactory {
    private static final Map<String, Marker> MARKERS = new ConcurrentHashMap<>();

    private MarkerFactory() {}

    public static Marker getMarker(String name) {
        return MARKERS.computeIfAbsent(name, BasicMarker::new);
    }

    public static Marker getDetachedMarker(String name) {
        return new BasicMarker(name);
    }

    private static class BasicMarker implements Marker {
        private final String name;

        BasicMarker(String name) {
            this.name = name;
        }

        @Override public String getName() { return name; }
        @Override public void add(Marker reference) {}
        @Override public boolean remove(Marker reference) { return false; }
        @Override public boolean hasChildren() { return false; }
        @Override public boolean hasReferences() { return false; }
        @Override public Iterator<Marker> iterator() { return Collections.emptyIterator(); }
        @Override public boolean contains(Marker other) { return equals(other); }
        @Override public boolean contains(String name) { return this.name.equals(name); }
    }
}
