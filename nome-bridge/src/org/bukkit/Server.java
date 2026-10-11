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
import org.bukkit.plugin.ServicesManager;
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
    OfflinePlayer getOfflinePlayer(String name);
    OfflinePlayer getOfflinePlayer(UUID id);
    OfflinePlayer[] getOfflinePlayers();
    PluginManager getPluginManager();
    ServicesManager getServicesManager();
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

    // Anti-Xray Control (Paper/Leaf Engine Mode 1)
    void setAntiXrayEnabled(boolean enabled);
    boolean isAntiXrayEnabled();

    boolean isPrimaryThread();
    org.bukkit.command.CommandMap getCommandMap();
    org.bukkit.command.PluginCommand getPluginCommand(String name);
    ConsoleCommandSender getConsoleSender();

    org.bukkit.plugin.messaging.Messenger getMessenger();
    UnsafeValues getUnsafe();
    boolean isStopping();
    File getWorldContainer();

    io.papermc.paper.threadedregions.scheduler.AsyncScheduler getAsyncScheduler();
    io.papermc.paper.threadedregions.scheduler.GlobalRegionScheduler getGlobalRegionScheduler();
    io.papermc.paper.threadedregions.scheduler.RegionScheduler getRegionScheduler();

    org.bukkit.util.CachedServerIcon getServerIcon();
    org.bukkit.util.CachedServerIcon loadServerIcon(File file) throws Exception;
    org.bukkit.util.CachedServerIcon loadServerIcon(java.awt.image.BufferedImage image) throws Exception;
}
