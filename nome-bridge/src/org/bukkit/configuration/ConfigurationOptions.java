package org.bukkit.configuration;

public class ConfigurationOptions {
    private final Configuration configuration;
    private boolean copyDefaults = false;

    protected ConfigurationOptions(Configuration configuration) {
        this.configuration = configuration;
    }

    public Configuration configuration() {
        return configuration;
    }

    public boolean copyDefaults() {
        return copyDefaults;
    }

    public ConfigurationOptions copyDefaults(boolean value) {
        this.copyDefaults = value;
        return this;
    }
}
