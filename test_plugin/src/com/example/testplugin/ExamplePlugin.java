package com.example.testplugin;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.ChunkSnapshot;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.permission.Permission;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;

public class ExamplePlugin extends JavaPlugin implements Listener {

    private Economy economy;
    private Permission permission;

    @Override
    public void onLoad() {
        getLogger().info("[ExamplePlugin] Plugin loaded on " + Bukkit.getName() + " (" + Bukkit.getVersion() + ")");
    }

    @Override
    public void onEnable() {
        getLogger().info("[ExamplePlugin] Enabling standard Bukkit/Paper plugin features on Native Core!");
        getServer().getPluginManager().registerEvents(this, this);

        // 1. Verify Native C++ Crafting from Java Bukkit API
        short[] res2x2 = new short[2];
        if (Bukkit.craft2x2((short) 17, (short) 0, (short) 0, (short) 0, res2x2)) {
            getLogger().info("[ExamplePlugin] Native C++ Crafting Verified: 1 Oak Log -> Item " + res2x2[0] + " x" + res2x2[1] + " (Planks)");
        }

        short[] grid9 = new short[] {
            5, 5, 5,
            0, 280, 0,
            0, 280, 0
        };
        short[] res3x3 = new short[2];
        if (Bukkit.craft3x3(grid9, res3x3)) {
            getLogger().info("[ExamplePlugin] Native C++ 3x3 Crafting Verified: 3 Planks + 2 Sticks -> Item " + res3x3[0] + " x" + res3x3[1] + " (Wooden Pickaxe)");
        }

        // 2. Verify Smart Chunk Manager & WorldGen API
        World world = getServer().getWorld("world");
        if (world != null) {
            Chunk spawnChunk = world.getChunkAt(0, 0);
            getLogger().info("[ExamplePlugin] Smart Chunk (0, 0) loaded: " + spawnChunk.isLoaded());

            ChunkSnapshot snapshot = spawnChunk.getChunkSnapshot();
            getLogger().info("[ExamplePlugin] ChunkSnapshot (0, 0) Highest Block at (8, 8): Y=" + snapshot.getHighestBlockYAt(8, 8));

            // Test asynchronous multi-chunk load
            world.loadChunk(1, 1, true);
            getLogger().info("[ExamplePlugin] Asynchronously Loaded Adjacent Chunk (1, 1): " + world.isChunkLoaded(1, 1));
            getLogger().info("[ExamplePlugin] Total Active World Chunks: " + world.getLoadedChunks().length);

            // 3. Verify Native C++ Entity Spawning into Native ECS
            Location spawnLoc = new Location(world, 10.5, 65.0, 10.5);
            int entityId = world.spawnEntity(spawnLoc, EntityType.ZOMBIE);
            getLogger().info("[ExamplePlugin] Native Entity Spawned in C++ ECS: Zombie ID #" + entityId);

            Location villageLoc = world.locateNearestStructure("village", spawnLoc);
            if (villageLoc != null) {
                getLogger().info("[ExamplePlugin] Located Procedural Village at: (" + villageLoc.getBlockX() + ", " + villageLoc.getBlockZ() + ")");
            }
        }

        // 4. Verify ProtocolLib Compatibility (Universal Packet Interception Layer for 3rd-Party Anticheat & Anti-Xray)
        ProtocolLibrary.getProtocolManager().addPacketListener(new PacketAdapter(this,
                PacketType.Play.Client.POSITION,
                PacketType.Play.Client.FLYING,
                PacketType.Play.Server.MAP_CHUNK) {
            @Override
            public void onPacketReceiving(PacketEvent event) {
                // Intercept incoming packets for Anticheat (GrimAC, Vulcan, etc.)
            }

            @Override
            public void onPacketSending(PacketEvent event) {
                // Intercept outgoing packets for Anti-Xray / Obfuscators
            }
        });
        getLogger().info("[ExamplePlugin] Hooked ProtocolLib ProtocolManager packet interception pipeline!");

        // 5. Verify Built-in Vault Economy & Permission Services
        RegisteredServiceProvider<Economy> ecoRsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (ecoRsp != null) {
            economy = ecoRsp.getProvider();
            getLogger().info("[ExamplePlugin] Hooked into Vault Economy Provider: " + economy.getName());
            economy.depositPlayer("Steve", 1000.0);
            getLogger().info("[ExamplePlugin] Steve's Balance after $1000 deposit: " + economy.format(economy.getBalance("Steve")));
        }

        RegisteredServiceProvider<Permission> permRsp = getServer().getServicesManager().getRegistration(Permission.class);
        if (permRsp != null) {
            permission = permRsp.getProvider();
            getLogger().info("[ExamplePlugin] Hooked into Vault Permission Provider: " + permission.getName());
            permission.playerAdd("world", "Steve", "nome.admin.fly");
            boolean hasPerm = permission.playerHas("world", "Steve", "nome.admin.fly");
            getLogger().info("[ExamplePlugin] Steve has 'nome.admin.fly' permission: " + hasPerm);
        }

        // 6. Verify Paper/Leaf Native Anti-Xray Engine Mode 1
        boolean antiXray = Bukkit.isAntiXrayEnabled();
        getLogger().info("[ExamplePlugin] Native Paper/Leaf Anti-Xray Engine Mode 1 Status: " + (antiXray ? "ACTIVE" : "DISABLED"));

        // Schedule recurring task
        getServer().getScheduler().runTaskTimer(this, () -> {
            // Periodic keepalive
        }, 20, 20);
    }

    @Override
    public void onDisable() {
        getLogger().info("[ExamplePlugin] Plugin disabled.");
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerJoin(PlayerJoinEvent event) {
        getLogger().info("[ExamplePlugin] Caught PlayerJoinEvent: " + event.getPlayer().getName());
        event.getPlayer().sendMessage("§a[Nome] Welcome " + event.getPlayer().getName() + " to the 100% Native C/C++/Rust Minecraft Server!");

        // Equip Diamond Sword in player inventory
        ItemStack diamondSword = new ItemStack(Material.DIAMOND_SWORD, 1);
        event.getPlayer().getInventory().setItemInMainHand(diamondSword);
        getLogger().info("[ExamplePlugin] Equipped Diamond Sword in player hand: " + event.getPlayer().getItemInHand().getType());
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Material type = event.getBlock().getType();
        getLogger().info("[ExamplePlugin] Caught BlockBreakEvent: " + event.getPlayer().getName() +
                " broke " + type + " at (" + event.getBlock().getX() + ", " + event.getBlock().getY() + ", " + event.getBlock().getZ() + ")");
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Location to = event.getTo();
        if (to != null && to.getY() > 300.0) {
            getLogger().warning("[Anticheat] Flagged Illegal Fly/Speed for " + event.getPlayer().getName() + " at Y=" + to.getY() + "! Cancelling movement (Rubberband setback).");
            event.setCancelled(true);
        }
    }
}
