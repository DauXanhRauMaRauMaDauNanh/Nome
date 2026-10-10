package org.bukkit;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.ServicesManager;
import org.bukkit.scheduler.BukkitScheduler;
import io.nome.bridge.NomeNativeBridge;

public final class Bukkit {
    private static Server server;

    private Bukkit() {}

    public static Server getServer() {
        return server;
    }

    public static void setServer(Server s) {
        if (server != null) {
            throw new UnsupportedOperationException("Cannot redefine singleton Server");
        }
        server = s;
    }

    public static String getName() { return server.getName(); }
    public static String getVersion() { return server.getVersion(); }
    public static String getBukkitVersion() { return server.getBukkitVersion(); }
    public static Collection<? extends Player> getOnlinePlayers() { return server.getOnlinePlayers(); }
    public static int getMaxPlayers() { return server.getMaxPlayers(); }
    public static int getPort() { return server.getPort(); }
    public static String getIp() { return server.getIp(); }
    public static boolean broadcastMessage(String message) { return server.broadcastMessage(message); }
    public static Player getPlayer(String name) { return server.getPlayer(name); }
    public static Player getPlayer(UUID id) { return server.getPlayer(id); }
    public static OfflinePlayer getOfflinePlayer(String name) { return server.getOfflinePlayer(name); }
    public static OfflinePlayer getOfflinePlayer(UUID id) { return server.getOfflinePlayer(id); }
    public static OfflinePlayer[] getOfflinePlayers() { return server.getOfflinePlayers(); }
    public static PluginManager getPluginManager() { return server.getPluginManager(); }
    public static ServicesManager getServicesManager() { return server.getServicesManager(); }
    public static BukkitScheduler getScheduler() { return server.getScheduler(); }
    public static List<World> getWorlds() { return server.getWorlds(); }
    public static World getWorld(String name) { return server.getWorld(name); }
    public static World getWorld(UUID uid) { return server.getWorld(uid); }
    public static Logger getLogger() { return server.getLogger(); }
    public static boolean dispatchCommand(CommandSender sender, String commandLine) { return server.dispatchCommand(sender, commandLine); }
    public static void reload() { server.reload(); }
    public static void shutdown() { server.shutdown(); }
    public static double[] getTPS() { return server.getTPS(); }
    public static double getMSPT() { return server.getMSPT(); }

    // Anti-Xray Engine Mode 1
    public static void setAntiXrayEnabled(boolean enabled) { server.setAntiXrayEnabled(enabled); }
    public static boolean isAntiXrayEnabled() { return server.isAntiXrayEnabled(); }

    // 100% Native C++ Crafting Engine Access for Java Plugins
    public static boolean craft2x2(short in0, short in1, short in2, short in3, short[] outResult) {
        return NomeNativeBridge.craft2x2(in0, in1, in2, in3, outResult);
    }

    public static boolean craft3x3(short[] grid9, short[] outResult) {
        return NomeNativeBridge.craft3x3(grid9, outResult);
    }
}
