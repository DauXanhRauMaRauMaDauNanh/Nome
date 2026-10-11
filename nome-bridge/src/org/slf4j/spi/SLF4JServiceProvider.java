package org.slf4j.spi;

import org.slf4j.ILoggerFactory;
import org.slf4j.LoggerFactory;

public interface SLF4JServiceProvider {
    ILoggerFactory getLoggerFactory();
    void initialize();
    String getRequestedApiVersion();
}
