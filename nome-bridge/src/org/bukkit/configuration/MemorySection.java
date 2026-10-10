package org.bukkit.configuration;

import java.util.*;

public class MemorySection implements ConfigurationSection {
    protected final Map<String, Object> map = new LinkedHashMap<>();
    private final Configuration root;
    private final ConfigurationSection parent;
    private final String path;

    protected MemorySection() {
        if (!(this instanceof Configuration)) {
            throw new IllegalStateException("Cannot construct a root MemorySection without a Configuration");
        }
        this.root = (Configuration) this;
        this.parent = null;
        this.path = "";
    }

    protected MemorySection(ConfigurationSection parent, String path) {
        this.root = parent.getRoot();
        this.parent = parent;
        this.path = path;
    }

    @Override
    public Set<String> getKeys(boolean deep) {
        return new LinkedHashSet<>(map.keySet());
    }

    @Override
    public Map<String, Object> getValues(boolean deep) {
        return new LinkedHashMap<>(map);
    }

    @Override
    public boolean contains(String path) {
        return get(path) != null;
    }

    @Override
    public boolean isSet(String path) {
        return map.containsKey(path);
    }

    @Override public String getCurrentPath() { return path; }
    @Override public String getName() { return path; }
    @Override public Configuration getRoot() { return root; }
    @Override public ConfigurationSection getParent() { return parent; }

    @Override
    public Object get(String path) {
        return get(path, null);
    }

    @Override
    public Object get(String path, Object def) {
        if (path == null) return def;
        int dot = path.indexOf('.');
        if (dot != -1) {
            String first = path.substring(0, dot);
            String rest = path.substring(dot + 1);
            Object obj = map.get(first);
            if (obj instanceof ConfigurationSection) {
                return ((ConfigurationSection) obj).get(rest, def);
            }
            return def;
        }
        Object val = map.get(path);
        return val != null ? val : def;
    }

    @Override
    public void set(String path, Object value) {
        if (path == null) return;
        int dot = path.indexOf('.');
        if (dot != -1) {
            String first = path.substring(0, dot);
            String rest = path.substring(dot + 1);
            ConfigurationSection sec = getConfigurationSection(first);
            if (sec == null) {
                sec = createSection(first);
            }
            sec.set(rest, value);
            return;
        }
        if (value == null) {
            map.remove(path);
        } else {
            map.put(path, value);
        }
    }

    @Override
    public ConfigurationSection createSection(String path) {
        MemorySection sec = new MemorySection(this, path);
        set(path, sec);
        return sec;
    }

    @Override
    public ConfigurationSection getConfigurationSection(String path) {
        Object val = get(path);
        if (val instanceof ConfigurationSection) {
            return (ConfigurationSection) val;
        }
        return null;
    }

    @Override
    public boolean isConfigurationSection(String path) {
        return get(path) instanceof ConfigurationSection;
    }

    @Override
    public String getString(String path) {
        return getString(path, null);
    }

    @Override
    public String getString(String path, String def) {
        Object val = get(path, def);
        return val != null ? val.toString() : def;
    }

    @Override
    public int getInt(String path) {
        return getInt(path, 0);
    }

    @Override
    public int getInt(String path, int def) {
        Object val = get(path, def);
        if (val instanceof Number) return ((Number) val).intValue();
        try {
            return val != null ? Integer.parseInt(val.toString()) : def;
        } catch (NumberFormatException e) {
            return def;
        }
    }

    @Override
    public boolean getBoolean(String path) {
        return getBoolean(path, false);
    }

    @Override
    public boolean getBoolean(String path, boolean def) {
        Object val = get(path, def);
        if (val instanceof Boolean) return (Boolean) val;
        return val != null ? Boolean.parseBoolean(val.toString()) : def;
    }

    @Override
    public double getDouble(String path) {
        return getDouble(path, 0.0);
    }

    @Override
    public double getDouble(String path, double def) {
        Object val = get(path, def);
        if (val instanceof Number) return ((Number) val).doubleValue();
        try {
            return val != null ? Double.parseDouble(val.toString()) : def;
        } catch (NumberFormatException e) {
            return def;
        }
    }

    @Override
    public long getLong(String path) {
        return getLong(path, 0L);
    }

    @Override
    public long getLong(String path, long def) {
        Object val = get(path, def);
        if (val instanceof Number) return ((Number) val).longValue();
        try {
            return val != null ? Long.parseLong(val.toString()) : def;
        } catch (NumberFormatException e) {
            return def;
        }
    }

    @Override
    public List<?> getList(String path) {
        return getList(path, null);
    }

    @Override
    public List<?> getList(String path, List<?> def) {
        Object val = get(path, def);
        return (val instanceof List<?>) ? (List<?>) val : def;
    }

    @Override
    public List<String> getStringList(String path) {
        List<?> list = getList(path);
        if (list == null) return Collections.emptyList();
        List<String> result = new ArrayList<>();
        for (Object object : list) {
            if (object != null) result.add(object.toString());
        }
        return result;
    }
}
