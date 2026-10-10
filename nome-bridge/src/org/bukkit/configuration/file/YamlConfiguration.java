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
            final String key;
            List<Object> currentList = null;

            StackEntry(int indent, ConfigurationSection section, String key) {
                this.indent = indent;
                this.section = section;
                this.key = key;
            }
        }

        Deque<StackEntry> stack = new ArrayDeque<>();
        stack.push(new StackEntry(-1, this, null));

        String[] lines = contents.split("\r?\n");
        for (String rawLine : lines) {
            String line = stripComment(rawLine);
            if (line.trim().isEmpty()) {
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
                if (current.currentList == null) {
                    current.currentList = new ArrayList<>();
                    if (stack.size() > 1) {
                        Iterator<StackEntry> it = stack.iterator();
                        it.next(); // current
                        if (it.hasNext()) {
                            StackEntry parent = it.next();
                            if (parent != null && current.key != null) {
                                parent.section.set(current.key, current.currentList);
                            }
                        }
                    }
                }
                current.currentList.add(parseScalar(itemStr));
                continue;
            }

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
                    ConfigurationSection newSec = current.section.createSection(key);
                    StackEntry nextEntry = new StackEntry(indent, newSec, key);
                    stack.push(nextEntry);
                } else {
                    current.section.set(key, parseScalar(valStr));
                }
            }
        }
    }

    private String stripComment(String line) {
        int hash = -1;
        boolean inQuotes = false;
        char quoteChar = 0;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if ((c == '"' || c == '\'') && (i == 0 || line.charAt(i - 1) != '\\')) {
                if (!inQuotes) {
                    inQuotes = true;
                    quoteChar = c;
                } else if (quoteChar == c) {
                    inQuotes = false;
                }
            } else if (c == '#' && !inQuotes) {
                hash = i;
                break;
            }
        }
        return hash != -1 ? line.substring(0, hash) : line;
    }

    private Object parseScalar(String s) {
        if (s == null || s.isEmpty()) return "";
        s = s.trim();
        if (s.startsWith("[") && s.endsWith("]")) {
            String inner = s.substring(1, s.length() - 1).trim();
            if (inner.isEmpty()) return new ArrayList<>();
            String[] parts = inner.split(",");
            List<Object> res = new ArrayList<>();
            for (String p : parts) {
                res.add(parseScalar(p.trim()));
            }
            return res;
        }
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
