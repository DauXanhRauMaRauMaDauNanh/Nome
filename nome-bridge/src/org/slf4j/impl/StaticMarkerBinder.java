package org.slf4j.impl;

import org.slf4j.IMarkerFactory;
import org.slf4j.MarkerFactory;

public final class StaticMarkerBinder {
    private static final StaticMarkerBinder SINGLETON = new StaticMarkerBinder();

    private StaticMarkerBinder() {}

    public static StaticMarkerBinder getSingleton() {
        return SINGLETON;
    }

    public String getMarkerFactoryClassStr() {
        return "org.slf4j.helpers.BasicMarkerFactory";
    }
}
