package org.slf4j.impl;

import org.slf4j.ILoggerFactory;
import org.slf4j.LoggerFactory;
import org.slf4j.spi.LoggerFactoryBinder;

public final class StaticLoggerBinder implements LoggerFactoryBinder {
    private static final StaticLoggerBinder SINGLETON = new StaticLoggerBinder();
    public static final String REQUESTED_API_VERSION = "1.7.36";

    private StaticLoggerBinder() {}

    public static StaticLoggerBinder getSingleton() {
        return SINGLETON;
    }

    @Override
    public ILoggerFactory getLoggerFactory() {
        return LoggerFactory.getILoggerFactory();
    }

    @Override
    public String getLoggerFactoryClassStr() {
        return "org.slf4j.helpers.NomeNativeLoggerFactory";
    }
}
