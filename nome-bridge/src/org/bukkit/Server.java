package org.bukkit;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.scheduler.BukkitScheduler;

public interface Server {
    String getName();
    String getVersion();
    String getBukkitVersion();
    Collection<? extends Player> getOnlinePlayers();
    int getMaxPlayers();
    int getPort();
    String getIp();
    String getServerName();
    String getServerId();
    boolean broadcastMessage(String message);
    Player getPlayer(String name);
    Player getPlayer(UUID id);
    PluginManager getPluginManager();
    BukkitScheduler getScheduler();
    List<World> getWorlds();
    World getWorld(String name);
    World getWorld(UUID uid);
    Logger getLogger();
    boolean dispatchCommand(CommandSender sender, String commandLine);
    void reload();
    void shutdown();
    double[] getTPS();
    double getMSPT();
}
