package org.bukkit.plugin;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

public class PluginDescriptionFile {
    private String name = "";
    private String version = "1.0.0";
    private String main = "";
    private String description = "";
    private String author = "Unknown";
    private final List<String> authors = new ArrayList<>();
    private final List<String> depend = new ArrayList<>();
    private final List<String> softDepend = new ArrayList<>();
    private final Map<String, Map<String, Object>> commands = new LinkedHashMap<>();

    public PluginDescriptionFile(InputStream stream) {
        YamlConfiguration config = YamlConfiguration.loadConfiguration(new InputStreamReader(stream, StandardCharsets.UTF_8));
        this.name = config.getString("name", "");
        this.version = config.getString("version", "1.0.0");
        this.main = config.getString("main", "");
        this.description = config.getString("description", "");
        this.author = config.getString("author", "Unknown");
        if (config.contains("authors")) {
            this.authors.addAll(config.getStringList("authors"));
        } else if (this.author != null) {
            this.authors.add(this.author);
        }
        if (config.contains("depend")) {
            this.depend.addAll(config.getStringList("depend"));
        }
        if (config.contains("softdepend")) {
            this.softDepend.addAll(config.getStringList("softdepend"));
        }
        ConfigurationSection cmdSec = config.getConfigurationSection("commands");
        if (cmdSec != null) {
            for (String cmdKey : cmdSec.getKeys(false)) {
                ConfigurationSection sub = cmdSec.getConfigurationSection(cmdKey);
                Map<String, Object> map = new LinkedHashMap<>();
                if (sub != null) {
                    for (String k : sub.getKeys(false)) {
                        map.put(k, sub.get(k));
                    }
                }
                this.commands.put(cmdKey, map);
            }
        }
    }

    public PluginDescriptionFile(String name, String version, String main, String description, String author) {
        this.name = name;
        this.version = version;
        this.main = main;
        this.description = description;
        this.author = author;
        if (author != null) this.authors.add(author);
    }

    public String getName() { return name; }
    public String getVersion() { return version; }
    public String getMain() { return main; }
    public String getDescription() { return description; }
    public String getAuthor() { return author; }
    public List<String> getAuthors() { return authors; }
    public List<String> getDepend() { return depend; }
    public List<String> getSoftDepend() { return softDepend; }
    public Map<String, Map<String, Object>> getCommands() { return commands; }
}
