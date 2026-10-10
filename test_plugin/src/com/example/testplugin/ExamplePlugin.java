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
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import net.milkbowl.vault.permission.Permission;

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

        // 2. Verify Native C++ Entity Spawning into Native ECS
        if (getServer().getWorld("world") != null) {
            Location spawnLoc = new Location(getServer().getWorld("world"), 10.5, 65.0, 10.5);
            int entityId = getServer().getWorld("world").spawnEntity(spawnLoc, EntityType.ZOMBIE);
            getLogger().info("[ExamplePlugin] Native Entity Spawned in C++ ECS: Zombie ID #" + entityId);

            // 3. Verify Native Structure Locator
            Location villageLoc = getServer().getWorld("world").locateNearestStructure("village", spawnLoc);
            if (villageLoc != null) {
                getLogger().info("[ExamplePlugin] Located Procedural Village at: (" + villageLoc.getBlockX() + ", " + villageLoc.getBlockZ() + ")");
            }
        }

        // 4. Verify Built-in Vault Economy & Permission Services
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

        // 5. Verify Paper/Leaf Native Anti-Xray Engine Mode 1
        boolean antiXray = Bukkit.isAntiXrayEnabled();
        getLogger().info("[ExamplePlugin] Native Paper/Leaf Anti-Xray Engine Mode 1 Status: " + (antiXray ? "ACTIVE" : "DISABLED"));

        // Schedule a recurring task using BukkitScheduler
        getServer().getScheduler().runTaskTimer(this, () -> {
            // Periodic keepalive / metrics
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
        // Anticheat Simulation: If player teleports or moves illegally (e.g. speedhack check)
        Location to = event.getTo();
        if (to != null && to.getY() > 300.0) {
            getLogger().warning("[Anticheat] Flagged Illegal Fly/Speed for " + event.getPlayer().getName() + " at Y=" + to.getY() + "! Cancelling movement (Rubberband setback).");
            event.setCancelled(true);
        }
    }
}
