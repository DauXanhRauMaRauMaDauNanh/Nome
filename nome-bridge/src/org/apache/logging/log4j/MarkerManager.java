package org.apache.logging.log4j;

import java.util.concurrent.ConcurrentHashMap;

public final class MarkerManager {
    private static final ConcurrentHashMap<String, Marker> MARKERS = new ConcurrentHashMap<>();

    public static Marker getMarker(String name) {
        return MARKERS.computeIfAbsent(name, Log4jMarker::new);
    }

    private static class Log4jMarker implements Marker {
        private final String name;
        Log4jMarker(String name) { this.name = name; }
        public String getName() { return name; }
        public Marker[] getParents() { return new Marker[0]; }
        public boolean hasParents() { return false; }
        public Marker addParents(Marker... markers) { return this; }
        public Marker setParents(Marker... markers) { return this; }
        public boolean isInstanceOf(Marker m) { return m != null && name.equals(m.getName()); }
        public boolean isInstanceOf(String name) { return this.name.equals(name); }
        public String toString() { return name; }
    }
}
