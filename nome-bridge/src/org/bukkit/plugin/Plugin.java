package org.bukkit.plugin;

import java.io.File;
import java.util.logging.Logger;
import org.bukkit.Server;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

public interface Plugin {
    void onLoad();
    void onEnable();
    void onDisable();
    boolean isEnabled();
    String getName();
    PluginDescriptionFile getDescription();
    File getDataFolder();
    Logger getLogger();
    Server getServer();
    boolean onCommand(CommandSender sender, Command command, String label, String[] args);
}
