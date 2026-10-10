package org.bukkit.plugin;

import java.util.ArrayList;
import java.util.List;

public class PluginDescriptionFile {
    private final String name;
    private final String version;
    private final String main;
    private final String description;
    private final String author;
    private final List<String> authors = new ArrayList<>();
    private final List<String> depend = new ArrayList<>();
    private final List<String> softDepend = new ArrayList<>();

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
}
