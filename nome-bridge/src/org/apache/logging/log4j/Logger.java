package org.apache.logging.log4j;

public interface Logger {
    String getName();

    boolean isTraceEnabled();
    void trace(String message);
    void trace(String message, Object... params);
    void trace(String message, Throwable t);

    boolean isDebugEnabled();
    void debug(String message);
    void debug(String message, Object... params);
    void debug(String message, Throwable t);

    boolean isInfoEnabled();
    void info(String message);
    void info(String message, Object... params);
    void info(String message, Throwable t);

    boolean isWarnEnabled();
    void warn(String message);
    void warn(String message, Object... params);
    void warn(String message, Throwable t);

    boolean isErrorEnabled();
    void error(String message);
    void error(String message, Object... params);
    void error(String message, Throwable t);

    boolean isFatalEnabled();
    void fatal(String message);
    void fatal(String message, Object... params);
    void fatal(String message, Throwable t);

    void log(Level level, String message);
    void log(Level level, String message, Object... params);
    void log(Level level, String message, Throwable t);
}
