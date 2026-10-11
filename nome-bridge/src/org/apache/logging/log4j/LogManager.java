package org.apache.logging.log4j;

import io.nome.bridge.NomeNativeBridge;
import org.slf4j.helpers.MessageFormatter;
import java.util.concurrent.ConcurrentHashMap;

public final class LogManager {
    private static final ConcurrentHashMap<String, Logger> LOGGERS = new ConcurrentHashMap<>();
    private static final Logger ROOT_LOGGER = getLogger("Root");

    public static Logger getRootLogger() {
        return ROOT_LOGGER;
    }

    public static Logger getLogger() {
        return getLogger(getCallerClassName());
    }

    public static Logger getLogger(String name) {
        if (name == null) name = "Default";
        return LOGGERS.computeIfAbsent(name, NomeLog4jLogger::new);
    }

    public static Logger getLogger(Class<?> clazz) {
        return getLogger(clazz != null ? clazz.getName() : "Default");
    }

    public static Logger getLogger(Object obj) {
        return getLogger(obj != null ? obj.getClass().getName() : "Default");
    }

    private static String getCallerClassName() {
        StackWalker walker = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);
        return walker.walk(frames -> frames
                .map(StackWalker.StackFrame::getClassName)
                .filter(name -> !name.equals(LogManager.class.getName()))
                .findFirst()
                .orElse("Default"));
    }

    private static class NomeLog4jLogger implements Logger {
        private final String name;

        NomeLog4jLogger(String name) {
            this.name = name;
        }

        public String getName() { return name; }

        public boolean isTraceEnabled() { return true; }
        public boolean isDebugEnabled() { return true; }
        public boolean isInfoEnabled() { return true; }
        public boolean isWarnEnabled() { return true; }
        public boolean isErrorEnabled() { return true; }
        public boolean isFatalEnabled() { return true; }

        public void trace(String message) { log(Level.TRACE, message); }
        public void trace(String message, Object... params) { log(Level.TRACE, message, params); }
        public void trace(String message, Throwable t) { log(Level.TRACE, message, t); }

        public void debug(String message) { log(Level.DEBUG, message); }
        public void debug(String message, Object... params) { log(Level.DEBUG, message, params); }
        public void debug(String message, Throwable t) { log(Level.DEBUG, message, t); }

        public void info(String message) { log(Level.INFO, message); }
        public void info(String message, Object... params) { log(Level.INFO, message, params); }
        public void info(String message, Throwable t) { log(Level.INFO, message, t); }

        public void warn(String message) { log(Level.WARN, message); }
        public void warn(String message, Object... params) { log(Level.WARN, message, params); }
        public void warn(String message, Throwable t) { log(Level.WARN, message, t); }

        public void error(String message) { log(Level.ERROR, message); }
        public void error(String message, Object... params) { log(Level.ERROR, message, params); }
        public void error(String message, Throwable t) { log(Level.ERROR, message, t); }

        public void fatal(String message) { log(Level.FATAL, message); }
        public void fatal(String message, Object... params) { log(Level.FATAL, message, params); }
        public void fatal(String message, Throwable t) { log(Level.FATAL, message, t); }

        public void log(Level level, String message) {
            int rustLvl = toRustLevel(level);
            NomeNativeBridge.rustLog(rustLvl, name, message != null ? message : "null");
        }

        public void log(Level level, String message, Object... params) {
            String formatted = MessageFormatter.arrayFormat(message, params).getMessage();
            int rustLvl = toRustLevel(level);
            NomeNativeBridge.rustLog(rustLvl, name, formatted);
        }

        public void log(Level level, String message, Throwable t) {
            String full = message;
            if (t != null) {
                full = full + "\n" + t.toString();
            }
            int rustLvl = toRustLevel(level);
            NomeNativeBridge.rustLog(rustLvl, name, full);
        }

        private int toRustLevel(Level level) {
            if (level == null) return 1;
            if (level.intLevel() >= Level.FATAL.intLevel() && level.intLevel() <= Level.ERROR.intLevel()) return 3;
            if (level.intLevel() <= Level.WARN.intLevel() && level.intLevel() > Level.INFO.intLevel()) return 2;
            if (level.intLevel() <= Level.DEBUG.intLevel()) return 0;
            return 1;
        }
    }
}
