package com.example.testplugin;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.Bukkit;
import org.bukkit.Material;

public class ExamplePlugin extends JavaPlugin implements Listener {

    @Override
    public void onLoad() {
        getLogger().info("[ExamplePlugin] Plugin loaded on " + Bukkit.getName() + " (" + Bukkit.getVersion() + ")");
    }

    @Override
    public void onEnable() {
        getLogger().info("[ExamplePlugin] Enabling standard Bukkit/Paper plugin features on Native Core!");
        getServer().getPluginManager().registerEvents(this, this);

        // Verify Native C++ Crafting from Java Bukkit API
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
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Material type = event.getBlock().getType();
        getLogger().info("[ExamplePlugin] Caught BlockBreakEvent: " + event.getPlayer().getName() +
                " broke " + type + " at (" + event.getBlock().getX() + ", " + event.getBlock().getY() + ", " + event.getBlock().getZ() + ")");
    }
}
