package org.apache.logging.log4j;

import java.io.Serializable;

public interface Marker extends Serializable {
    String getName();
    Marker[] getParents();
    boolean hasParents();
    Marker addParents(Marker... markers);
    Marker setParents(Marker... markers);
    boolean isInstanceOf(Marker m);
    boolean isInstanceOf(String name);
}
