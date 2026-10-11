package org.apache.logging.log4j;

import java.io.Serializable;

public final class Level implements Comparable<Level>, Serializable {
    public static final Level OFF   = new Level("OFF", 0);
    public static final Level FATAL = new Level("FATAL", 100);
    public static final Level ERROR = new Level("ERROR", 200);
    public static final Level WARN  = new Level("WARN", 300);
    public static final Level INFO  = new Level("INFO", 400);
    public static final Level DEBUG = new Level("DEBUG", 500);
    public static final Level TRACE = new Level("TRACE", 600);
    public static final Level ALL   = new Level("ALL", Integer.MAX_VALUE);

    private final String name;
    private final int intLevel;

    private Level(String name, int intLevel) {
        this.name = name;
        this.intLevel = intLevel;
    }

    public String name() { return name; }
    public int intLevel() { return intLevel; }

    public static Level toLevel(String s) {
        return toLevel(s, INFO);
    }

    public static Level toLevel(String s, Level defaultLevel) {
        if (s == null) return defaultLevel;
        switch (s.toUpperCase()) {
            case "OFF": return OFF;
            case "FATAL": return FATAL;
            case "ERROR": return ERROR;
            case "WARN": return WARN;
            case "INFO": return INFO;
            case "DEBUG": return DEBUG;
            case "TRACE": return TRACE;
            case "ALL": return ALL;
            default: return defaultLevel;
        }
    }

    @Override
    public int compareTo(Level o) {
        return Integer.compare(this.intLevel, o.intLevel);
    }

    @Override
    public String toString() { return name; }
    @Override
    public boolean equals(Object o) {
        return (o instanceof Level) && ((Level) o).intLevel == this.intLevel;
    }
    @Override
    public int hashCode() { return intLevel; }
}
