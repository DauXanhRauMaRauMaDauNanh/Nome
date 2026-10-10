package org.bukkit.plugin.java;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;
import org.bukkit.Server;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabExecutor;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;

public abstract class JavaPlugin implements Plugin, TabExecutor {

    private boolean isEnabled = false;
    private PluginDescriptionFile description;
    private File dataFolder;
    private Logger logger;
    private Server server;

    public void init(PluginDescriptionFile desc, File dataFolder, Server server) {
        this.description = desc;
        this.dataFolder = dataFolder;
        this.server = server;
        this.logger = Logger.getLogger(desc.getName());
    }

    @Override public void onLoad() {}
    @Override public void onEnable() {}
    @Override public void onDisable() {}

    @Override
    public boolean isEnabled() {
        return isEnabled;
    }

    public void setEnabled(boolean enabled) {
        if (this.isEnabled != enabled) {
            this.isEnabled = enabled;
            if (enabled) {
                onEnable();
            } else {
                onDisable();
            }
        }
    }

    @Override public String getName() { return description != null ? description.getName() : getClass().getSimpleName(); }
    @Override public PluginDescriptionFile getDescription() { return description; }
    @Override public File getDataFolder() { return dataFolder; }
    @Override public Logger getLogger() { return logger; }
    @Override public Server getServer() { return server; }

    public PluginCommand getCommand(String name) {
        PluginCommand cmd = getServer().getPluginCommand(name);
        if (cmd == null) {
            cmd = new PluginCommand(name, this);
            getServer().getCommandMap().register(getName(), cmd);
        }
        return cmd;
    }

    private File configFile;
    private org.bukkit.configuration.file.FileConfiguration newConfig = null;

    @Override
    public org.bukkit.configuration.file.FileConfiguration getConfig() {
        if (newConfig == null) {
            reloadConfig();
        }
        return newConfig;
    }

    @Override
    public void reloadConfig() {
        if (configFile == null) {
            configFile = new File(getDataFolder(), "config.yml");
        }
        newConfig = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(configFile);
        java.io.InputStream defConfigStream = getResource("config.yml");
        if (defConfigStream != null) {
            org.bukkit.configuration.file.YamlConfiguration defConfig = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(defConfigStream);
            newConfig.setDefaults(defConfig);
        }
    }

    @Override
    public void saveConfig() {
        try {
            getConfig().save(configFile != null ? configFile : new File(getDataFolder(), "config.yml"));
        } catch (Exception ex) {
            getLogger().severe("Could not save config to " + configFile + ": " + ex.getMessage());
        }
    }

    @Override
    public void saveDefaultConfig() {
        if (configFile == null) {
            configFile = new File(getDataFolder(), "config.yml");
        }
        if (!configFile.exists()) {
            saveResource("config.yml", false);
        }
    }

    @Override
    public void saveResource(String resourcePath, boolean replace) {
        if (resourcePath == null || resourcePath.isEmpty()) {
            throw new IllegalArgumentException("ResourcePath cannot be null or empty");
        }
        resourcePath = resourcePath.replace('\\', '/');
        java.io.InputStream in = getResource(resourcePath);
        if (in == null) {
            return;
        }
        File outFile = new File(getDataFolder(), resourcePath);
        int lastIndex = resourcePath.lastIndexOf('/');
        File outDir = new File(getDataFolder(), resourcePath.substring(0, Math.max(lastIndex, 0)));
        if (!outDir.exists()) {
            outDir.mkdirs();
        }
        try {
            if (!outFile.exists() || replace) {
                try (java.io.OutputStream out = new java.io.FileOutputStream(outFile)) {
                    byte[] buf = new byte[1024];
                    int len;
                    while ((len = in.read(buf)) > 0) {
                        out.write(buf, 0, len);
                    }
                }
            }
        } catch (Exception ex) {
            getLogger().severe("Could not save " + outFile.getName() + " to " + outFile + ": " + ex.getMessage());
        }
    }

    @Override
    public java.io.InputStream getResource(String filename) {
        if (filename == null) return null;
        try {
            return getClass().getClassLoader().getResourceAsStream(filename);
        } catch (Exception ex) {
            return null;
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return Collections.emptyList();
    }
}
