package io.nome.bridge;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.SimplePluginManager;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.SimpleScheduler;

public class NomeServer implements Server {

    private static NomeServer instance;

    private final String name = "Nome";
    private final String version = "26.3-NATIVE (Protocol 777)";
    private final String bukkitVersion = "1.21.4-R0.1-SNAPSHOT";
    private final Logger logger = Logger.getLogger("Minecraft");
    private final SimplePluginManager pluginManager;
    private final SimpleScheduler scheduler;
    private final Map<String, World> worlds = new ConcurrentHashMap<>();
    private final Map<UUID, Player> players = new ConcurrentHashMap<>();
    private boolean isRunning = false;

    public NomeServer() {
        instance = this;
        Bukkit.setServer(this);

        this.pluginManager = new SimplePluginManager(this, new File("plugins"));
        this.scheduler = new SimpleScheduler();

        // Initialize default worlds
        World overworld = new World("world");
        worlds.put("world", overworld);
    }

    public static NomeServer getInstance() {
        return instance;
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
        players.put(steve.getUniqueId(), steve);
        overworld.addPlayer(steve);

        System.out.println("[Nome] [Native->Plugin EventBridge] Emitting PlayerJoinEvent...");
        pluginManager.callEvent(new PlayerJoinEvent(steve, "§e" + steve.getName() + " joined the game"));

        System.out.println("[Nome] [Native->Plugin EventBridge] Emitting BlockBreakEvent...");
        pluginManager.callEvent(new BlockBreakEvent(overworld.getBlockAt(0, 64, 0), steve));

        System.out.println("[Nome] Server ready! Native engine ticking at 20 TPS on port 25565.");
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
    @Override public PluginManager getPluginManager() { return pluginManager; }
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
}
