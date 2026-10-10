package io.nome.bridge;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.ServicesManager;
import org.bukkit.plugin.SimplePluginManager;
import org.bukkit.plugin.SimpleServicesManager;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.SimpleScheduler;
import io.nome.bridge.vault.NomeEconomy;
import io.nome.bridge.vault.NomePermission;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.permission.Permission;

public class NomeServer implements Server {

    private static NomeServer instance;

    private final String name = "Nome";
    private final String version = "26.3-NATIVE (Protocol 777)";
    private final String bukkitVersion = "1.21.4-R0.1-SNAPSHOT";
    private final Logger logger = Logger.getLogger("Minecraft");
    private final SimplePluginManager pluginManager;
    private final SimpleServicesManager servicesManager;
    private final SimpleScheduler scheduler;
    private final Map<String, World> worlds = new ConcurrentHashMap<>();
    private final Map<UUID, Player> players = new ConcurrentHashMap<>();
    private final Map<Integer, Player> playersById = new ConcurrentHashMap<>();
    private final Map<String, OfflinePlayer> offlinePlayers = new ConcurrentHashMap<>();
    private boolean isRunning = false;

    public NomeServer() {
        instance = this;
        Bukkit.setServer(this);

        this.pluginManager = new SimplePluginManager(this, new File("plugins"));
        this.servicesManager = new SimpleServicesManager();
        this.scheduler = new SimpleScheduler();

        // Initialize default worlds
        World overworld = new World("world");
        worlds.put("world", overworld);

        // Register built-in Vault Economy & Permission providers
        registerBuiltinServices();
    }

    private void registerBuiltinServices() {
        try {
            NomeEconomy economy = new NomeEconomy();
            servicesManager.register(Economy.class, economy, null, ServicePriority.Normal);
            System.out.println("[Nome-Vault] Registered native built-in Economy provider: " + economy.getName());

            NomePermission permission = new NomePermission();
            servicesManager.register(Permission.class, permission, null, ServicePriority.Normal);
            System.out.println("[Nome-Vault] Registered native built-in Permission provider: " + permission.getName());
        } catch (Throwable t) {
            System.err.println("[Nome-Vault] Note: Service registration note: " + t.getMessage());
        }
    }

    public static NomeServer getInstance() {
        return instance;
    }

    public void registerPlayer(Player player) {
        if (player == null) return;
        players.put(player.getUniqueId(), player);
        playersById.put(player.getEntityId(), player);
        offlinePlayers.put(player.getName().toLowerCase(), player);
        World w = player.getWorld();
        if (w != null) w.addPlayer(player);
    }

    public void unregisterPlayer(Player player) {
        if (player == null) return;
        players.remove(player.getUniqueId());
        playersById.remove(player.getEntityId());
        World w = player.getWorld();
        if (w != null) w.removePlayer(player);
    }

    public Player getPlayerById(int id) {
        return playersById.get(id);
    }

    public void start() {
        System.out.println("==================================================");
        System.out.println("   _  __                         ____             ");
        System.out.println("  / |/ /__  __ _  ___           / __/__ _____  __ ");
        System.out.println(" /    / _ \\/  ' \\/ -_)         _\\ \\/ -_) __/ |/ / ");
        System.out.println("/_/|_/\\___/_/_/_/\\__/         /___/\\__/_/  |___/  ");
        System.out.println("==================================================");
        System.out.println("[Nome] Starting engine: " + version);
        System.out.println("[Nome] Native Core: " + NomeNativeBridge.getVersion());

        // 1. Initialize and start native C/C++/Rust engine (starts TCP listener on 25565)
        NomeNativeBridge.init();
        NomeNativeBridge.start();
        isRunning = true;

        // 2. Load Java Plugins
        System.out.println("[Nome] Loading Bukkit/Paper Java Plugins...");
        List<Plugin> loaded = pluginManager.loadPlugins();
        for (Plugin p : loaded) {
            pluginManager.enablePlugin(p);
            System.out.println("[Nome] Enabled plugin: " + p.getName() + " v" + p.getDescription().getVersion());
        }

        // 3. Test Event Bridge by simulating native event triggers to Java Plugins
        World overworld = worlds.get("world");
        Location spawnLoc = new Location(overworld, 0.5, 64.0, 0.5);
        Player steve = new Player(1, "Steve", UUID.randomUUID(), overworld, spawnLoc, this);
        registerPlayer(steve);

        System.out.println("[Nome] [Native->Plugin EventBridge] Emitting PlayerJoinEvent...");
        pluginManager.callEvent(new PlayerJoinEvent(steve, "§e" + steve.getName() + " joined the game"));

        System.out.println("[Nome] [Native->Plugin EventBridge] Emitting BlockBreakEvent...");
        pluginManager.callEvent(new BlockBreakEvent(overworld.getBlockAt(0, 64, 0), steve));

        System.out.println("[Nome] Server ready! Native engine ticking at 20 TPS on port 25565.");
        System.out.println("[Nome] Native Anti-Xray Engine Mode 1: " + (isAntiXrayEnabled() ? "ENABLED" : "DISABLED"));
    }

    public void tick() {
        if (!isRunning) return;
        NomeNativeBridge.tickOnce();
    }

    public void stop() {
        shutdown();
    }

    @Override
    public void shutdown() {
        System.out.println("[Nome] Stopping server...");
        isRunning = false;
        pluginManager.clearPlugins();
        NomeNativeBridge.stop();
        System.out.println("[Nome] Server stopped.");
    }

    @Override public String getName() { return name; }
    @Override public String getVersion() { return version; }
    @Override public String getBukkitVersion() { return bukkitVersion; }
    @Override public Collection<? extends Player> getOnlinePlayers() { return players.values(); }
    @Override public int getMaxPlayers() { return 100; }
    @Override public int getPort() { return 25565; }
    @Override public String getIp() { return "0.0.0.0"; }
    @Override public String getServerName() { return "Nome Minecraft Server"; }
    @Override public String getServerId() { return "nome"; }

    @Override
    public boolean broadcastMessage(String message) {
        System.out.println("[Broadcast] " + message);
        NomeNativeBridge.broadcastMessage(message);
        return true;
    }

    @Override public Player getPlayer(String name) {
        for (Player p : players.values()) {
            if (p.getName().equalsIgnoreCase(name)) return p;
        }
        return null;
    }

    @Override public Player getPlayer(UUID id) { return players.get(id); }

    @Override
    public OfflinePlayer getOfflinePlayer(String name) {
        if (name == null) return null;
        Player online = getPlayer(name);
        if (online != null) return online;
        return offlinePlayers.get(name.toLowerCase());
    }

    @Override
    public OfflinePlayer getOfflinePlayer(UUID id) {
        if (id == null) return null;
        Player online = players.get(id);
        if (online != null) return online;
        for (OfflinePlayer p : offlinePlayers.values()) {
            if (p.getUniqueId().equals(id)) return p;
        }
        return null;
    }

    @Override
    public OfflinePlayer[] getOfflinePlayers() {
        return offlinePlayers.values().toArray(new OfflinePlayer[0]);
    }

    @Override public PluginManager getPluginManager() { return pluginManager; }
    @Override public ServicesManager getServicesManager() { return servicesManager; }
    @Override public BukkitScheduler getScheduler() { return scheduler; }
    @Override public List<World> getWorlds() { return new ArrayList<>(worlds.values()); }
    @Override public World getWorld(String name) { return worlds.get(name); }
    @Override public World getWorld(UUID uid) {
        for (World w : worlds.values()) {
            if (w.getUID().equals(uid)) return w;
        }
        return null;
    }

    @Override public Logger getLogger() { return logger; }

    @Override
    public boolean dispatchCommand(CommandSender sender, String commandLine) {
        System.out.println("[Command] " + sender.getName() + " executed: /" + commandLine);
        return true;
    }

    @Override public void reload() {
        System.out.println("[Nome] Reloading server plugins...");
        pluginManager.clearPlugins();
        pluginManager.loadPlugins();
    }

    @Override
    public double[] getTPS() {
        double current = NomeNativeBridge.tickOnce();
        return new double[]{current, current, current};
    }

    @Override
    public double getMSPT() {
        return NomeNativeBridge.getMspt();
    }

    @Override
    public void setAntiXrayEnabled(boolean enabled) {
        NomeNativeBridge.setAntiXray(enabled);
    }

    @Override
    public boolean isAntiXrayEnabled() {
        return NomeNativeBridge.isAntiXray();
    }
}
