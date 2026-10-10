package org.bukkit.configuration.file;

import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class YamlConfiguration extends FileConfiguration {

    public YamlConfiguration() {
        super();
    }

    public YamlConfiguration(Configuration defaults) {
        super(defaults);
    }

    @Override
    public YamlConfigurationOptions options() {
        if (options == null) {
            options = new YamlConfigurationOptions(this);
        }
        return (YamlConfigurationOptions) options;
    }

    @Override
    public String saveToString() {
        StringBuilder sb = new StringBuilder();
        String header = options().header();
        if (header != null && !header.isEmpty()) {
            for (String line : header.split("\n")) {
                sb.append("# ").append(line).append("\n");
            }
            sb.append("\n");
        }
        dumpSection(sb, this, 0);
        return sb.toString();
    }

    private void dumpSection(StringBuilder sb, ConfigurationSection sec, int indentLevel) {
        String indent = "  ".repeat(indentLevel);
        for (String key : sec.getKeys(false)) {
            Object val = sec.get(key);
            if (val instanceof ConfigurationSection) {
                sb.append(indent).append(key).append(":\n");
                dumpSection(sb, (ConfigurationSection) val, indentLevel + 1);
            } else if (val instanceof List<?>) {
                sb.append(indent).append(key).append(":\n");
                for (Object item : (List<?>) val) {
                    sb.append(indent).append("  - ").append(formatScalar(item)).append("\n");
                }
            } else {
                sb.append(indent).append(key).append(": ").append(formatScalar(val)).append("\n");
            }
        }
    }

    private String formatScalar(Object val) {
        if (val == null) return "''";
        if (val instanceof Boolean || val instanceof Number) {
            return val.toString();
        }
        String str = val.toString();
        if (str.isEmpty()) return "''";
        if (str.equalsIgnoreCase("true") || str.equalsIgnoreCase("false") ||
            str.matches("-?\\d+(\\.\\d+)?") ||
            str.contains(":") || str.contains("#") || str.contains("'") || str.contains("\"") ||
            str.startsWith(" ") || str.endsWith(" ")) {
            return "\"" + str.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
        }
        return str;
    }

    @Override
    public void loadFromString(String contents) throws InvalidConfigurationException {
        if (contents == null || contents.isEmpty()) return;
        this.map.clear();

        class StackEntry {
            final int indent;
            final ConfigurationSection section;
            String currentListKey = null;
            List<Object> currentList = null;

            StackEntry(int indent, ConfigurationSection section) {
                this.indent = indent;
                this.section = section;
            }
        }

        Deque<StackEntry> stack = new ArrayDeque<>();
        stack.push(new StackEntry(-1, this));

        String[] lines = contents.split("\r?\n");
        for (String line : lines) {
            if (line.trim().isEmpty() || line.trim().startsWith("#")) {
                continue;
            }

            int indent = 0;
            while (indent < line.length() && line.charAt(indent) == ' ') {
                indent++;
            }
            String trimmed = line.trim();

            while (stack.size() > 1 && stack.peek().indent >= indent) {
                stack.pop();
            }
            StackEntry current = stack.peek();

            if (trimmed.startsWith("- ")) {
                String itemStr = trimmed.substring(2).trim();
                if (current.currentListKey != null && current.currentList != null) {
                    current.currentList.add(parseScalar(itemStr));
                }
                continue;
            }

            // Not a list item anymore for the current section
            current.currentListKey = null;
            current.currentList = null;

            int colon = trimmed.indexOf(':');
            if (colon != -1) {
                String key = trimmed.substring(0, colon).trim();
                if (key.startsWith("\"") && key.endsWith("\"") && key.length() >= 2) {
                    key = key.substring(1, key.length() - 1);
                } else if (key.startsWith("'") && key.endsWith("'") && key.length() >= 2) {
                    key = key.substring(1, key.length() - 1);
                }

                String valStr = trimmed.substring(colon + 1).trim();
                if (valStr.isEmpty()) {
                    // Check if next lines could be list or nested section
                    ConfigurationSection newSec = current.section.createSection(key);
                    StackEntry nextEntry = new StackEntry(indent, newSec);
                    // Also prepare list in case next line is '- '
                    List<Object> list = new ArrayList<>();
                    nextEntry.currentListKey = key;
                    nextEntry.currentList = list;
                    current.section.set(key, list);
                    stack.push(nextEntry);
                } else {
                    current.section.set(key, parseScalar(valStr));
                }
            }
        }
    }

    private Object parseScalar(String s) {
        if (s == null || s.isEmpty()) return "";
        if (s.startsWith("\"") && s.endsWith("\"") && s.length() >= 2) {
            return s.substring(1, s.length() - 1).replace("\\\"", "\"").replace("\\\\", "\\");
        }
        if (s.startsWith("'") && s.endsWith("'") && s.length() >= 2) {
            return s.substring(1, s.length() - 1).replace("''", "'");
        }
        if (s.equalsIgnoreCase("true")) return Boolean.TRUE;
        if (s.equalsIgnoreCase("false")) return Boolean.FALSE;
        try {
            if (s.contains(".")) {
                return Double.parseDouble(s);
            } else {
                return Long.parseLong(s);
            }
        } catch (NumberFormatException ignored) {
        }
        return s;
    }

    public static YamlConfiguration loadConfiguration(File file) {
        YamlConfiguration config = new YamlConfiguration();
        try {
            if (file != null && file.exists()) {
                config.load(file);
            }
        } catch (Exception ex) {
            System.err.println("[Nome-Config] Failed to load configuration from " + file + ": " + ex.getMessage());
        }
        return config;
    }

    public static YamlConfiguration loadConfiguration(Reader reader) {
        YamlConfiguration config = new YamlConfiguration();
        try {
            if (reader != null) {
                config.load(reader);
            }
        } catch (Exception ex) {
            System.err.println("[Nome-Config] Failed to load configuration from reader: " + ex.getMessage());
        }
        return config;
    }

    public static YamlConfiguration loadConfiguration(InputStream stream) {
        if (stream == null) return new YamlConfiguration();
        return loadConfiguration(new InputStreamReader(stream, StandardCharsets.UTF_8));
    }
}
