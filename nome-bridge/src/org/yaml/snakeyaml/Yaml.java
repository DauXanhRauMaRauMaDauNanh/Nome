package org.yaml.snakeyaml;

import org.bukkit.configuration.file.YamlConfiguration;
import java.io.InputStream;
import java.io.Reader;
import java.util.Map;

public class Yaml {
    public Object load(String yaml) {
        if (yaml == null || yaml.trim().isEmpty()) return null;
        YamlConfiguration config = new YamlConfiguration();
        try {
            config.loadFromString(yaml);
            return config.getValues(false);
        } catch (Exception e) {
            return null;
        }
    }

    public Object load(InputStream io) {
        if (io == null) return null;
        YamlConfiguration config = YamlConfiguration.loadConfiguration(io);
        return config.getValues(false);
    }

    public Object load(Reader io) {
        if (io == null) return null;
        YamlConfiguration config = YamlConfiguration.loadConfiguration(io);
        return config.getValues(false);
    }

    public String dump(Object data) {
        if (data == null) return "null\n";
        if (data instanceof Map<?, ?> map) {
            YamlConfiguration config = new YamlConfiguration();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                config.set(String.valueOf(entry.getKey()), entry.getValue());
            }
            return config.saveToString();
        }
        return data.toString() + "\n";
    }
}
