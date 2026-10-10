package org.bukkit.plugin.java;

import java.io.File;
import java.util.logging.Logger;
import org.bukkit.Server;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;

public abstract class JavaPlugin implements Plugin {

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

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        return false;
    }
}
