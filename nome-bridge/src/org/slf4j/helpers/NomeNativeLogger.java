package org.slf4j.helpers;

import org.slf4j.Logger;
import io.nome.bridge.NomeNativeBridge;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * 100% Native Rust-backed Logger implementation for SLF4J.
 * Directly routes all logs through Panama FFM into nome_rust_log in C/C++/Rust.
 * Zero Log4j2 dependency, zero JVM heap logs buffer.
 */
public class NomeNativeLogger implements Logger {

    private final String name;

    public NomeNativeLogger(String name) {
        this.name = name != null ? name : "Nome";
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean isTraceEnabled() {
        return false;
    }

    @Override
    public void trace(String msg) {}

    @Override
    public void trace(String format, Object arg) {}

    @Override
    public void trace(String format, Object arg1, Object arg2) {}

    @Override
    public void trace(String format, Object... arguments) {}

    @Override
    public void trace(String msg, Throwable t) {}

    @Override
    public boolean isDebugEnabled() {
        return true;
    }

    @Override
    public void debug(String msg) {
        log(1, msg, null);
    }

    @Override
    public void debug(String format, Object arg) {
        FormattingTuple ft = MessageFormatter.format(format, arg);
        log(1, ft.getMessage(), ft.getThrowable());
    }

    @Override
    public void debug(String format, Object arg1, Object arg2) {
        FormattingTuple ft = MessageFormatter.format(format, arg1, arg2);
        log(1, ft.getMessage(), ft.getThrowable());
    }

    @Override
    public void debug(String format, Object... arguments) {
        FormattingTuple ft = MessageFormatter.arrayFormat(format, arguments);
        log(1, ft.getMessage(), ft.getThrowable());
    }

    @Override
    public void debug(String msg, Throwable t) {
        log(1, msg, t);
    }

    @Override
    public boolean isInfoEnabled() {
        return true;
    }

    @Override
    public void info(String msg) {
        log(2, msg, null);
    }

    @Override
    public void info(String format, Object arg) {
        FormattingTuple ft = MessageFormatter.format(format, arg);
        log(2, ft.getMessage(), ft.getThrowable());
    }

    @Override
    public void info(String format, Object arg1, Object arg2) {
        FormattingTuple ft = MessageFormatter.format(format, arg1, arg2);
        log(2, ft.getMessage(), ft.getThrowable());
    }

    @Override
    public void info(String format, Object... arguments) {
        FormattingTuple ft = MessageFormatter.arrayFormat(format, arguments);
        log(2, ft.getMessage(), ft.getThrowable());
    }

    @Override
    public void info(String msg, Throwable t) {
        log(2, msg, t);
    }

    @Override
    public boolean isWarnEnabled() {
        return true;
    }

    @Override
    public void warn(String msg) {
        log(3, msg, null);
    }

    @Override
    public void warn(String format, Object arg) {
        FormattingTuple ft = MessageFormatter.format(format, arg);
        log(3, ft.getMessage(), ft.getThrowable());
    }

    @Override
    public void warn(String format, Object arg1, Object arg2) {
        FormattingTuple ft = MessageFormatter.format(format, arg1, arg2);
        log(3, ft.getMessage(), ft.getThrowable());
    }

    @Override
    public void warn(String format, Object... arguments) {
        FormattingTuple ft = MessageFormatter.arrayFormat(format, arguments);
        log(3, ft.getMessage(), ft.getThrowable());
    }

    @Override
    public void warn(String msg, Throwable t) {
        log(3, msg, t);
    }

    @Override
    public boolean isErrorEnabled() {
        return true;
    }

    @Override
    public void error(String msg) {
        log(4, msg, null);
    }

    @Override
    public void error(String format, Object arg) {
        FormattingTuple ft = MessageFormatter.format(format, arg);
        log(4, ft.getMessage(), ft.getThrowable());
    }

    @Override
    public void error(String format, Object arg1, Object arg2) {
        FormattingTuple ft = MessageFormatter.format(format, arg1, arg2);
        log(4, ft.getMessage(), ft.getThrowable());
    }

    @Override
    public void error(String format, Object... arguments) {
        FormattingTuple ft = MessageFormatter.arrayFormat(format, arguments);
        log(4, ft.getMessage(), ft.getThrowable());
    }

    @Override
    public void error(String msg, Throwable t) {
        log(4, msg, t);
    }

    private void log(int level, String msg, Throwable t) {
        if (msg == null) msg = "";
        if (t != null) {
            StringWriter sw = new StringWriter();
            t.printStackTrace(new PrintWriter(sw));
            msg = msg + "\n" + sw.toString();
        }
        NomeNativeBridge.rustLog(level, name, msg);
    }
}
