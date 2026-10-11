package org.slf4j;

import org.slf4j.helpers.NomeNativeLogger;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * High performance, zero-dependency SLF4J LoggerFactory.
 * Provides instant access to native Rust logging without Log4j2 or heavy Java logging stacks.
 */
public final class LoggerFactory {

    private static final ConcurrentMap<String, Logger> LOGGERS = new ConcurrentHashMap<>();
    private static final ILoggerFactory FACTORY = LoggerFactory::getLogger;

    private LoggerFactory() {}

    public static Logger getLogger(String name) {
        if (name == null) name = "ROOT";
        return LOGGERS.computeIfAbsent(name, NomeNativeLogger::new);
    }

    public static Logger getLogger(Class<?> clazz) {
        return getLogger(clazz != null ? clazz.getSimpleName() : "ROOT");
    }

    public static ILoggerFactory getILoggerFactory() {
        return FACTORY;
    }
}
