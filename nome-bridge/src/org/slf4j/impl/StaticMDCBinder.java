package org.slf4j.impl;

public final class StaticMDCBinder {
    private static final StaticMDCBinder SINGLETON = new StaticMDCBinder();

    private StaticMDCBinder() {}

    public static StaticMDCBinder getSingleton() {
        return SINGLETON;
    }

    public String getMDCAdapterClassStr() {
        return "org.slf4j.helpers.BasicMDCAdapter";
    }
}
