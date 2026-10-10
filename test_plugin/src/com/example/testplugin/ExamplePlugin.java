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
import org.bukkit.World;
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
    }

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);

        // 1. Hook ProtocolLib Packet Interception Pipeline
        ProtocolLibrary.getProtocolManager().addPacketListener(new PacketAdapter(this,
                PacketType.Play.Client.POSITION,
                PacketType.Play.Client.FLYING,
                PacketType.Play.Server.MAP_CHUNK) {
            @Override
            public void onPacketReceiving(PacketEvent event) {
                // Intercept incoming client packets (e.g. Anticheat analysis)
            }

            @Override
            public void onPacketSending(PacketEvent event) {
                // Intercept outgoing server packets (e.g. Anti-Xray obfuscation)
            }
        });

        // 2. Hook Vault Services
        RegisteredServiceProvider<Economy> ecoRsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (ecoRsp != null) {
            economy = ecoRsp.getProvider();
        }

        RegisteredServiceProvider<Permission> permRsp = getServer().getServicesManager().getRegistration(Permission.class);
        if (permRsp != null) {
            permission = permRsp.getProvider();
        }

        getLogger().info("Plugin enabled successfully! Hooked into Vault & ProtocolLib.");
    }

    @Override
    public void onDisable() {
        getLogger().info("Plugin disabled.");
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerJoin(PlayerJoinEvent event) {
        event.getPlayer().sendMessage("§a[Nome] Welcome " + event.getPlayer().getName() + " to the server!");

        // Give starter Diamond Sword in player main hand
        ItemStack diamondSword = new ItemStack(Material.DIAMOND_SWORD, 1);
        event.getPlayer().getInventory().setItemInMainHand(diamondSword);
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Location to = event.getTo();
        if (to != null && to.getY() > 300.0) {
            getLogger().warning("[Anticheat] Flagged Illegal Fly/Speed for " + event.getPlayer().getName() + " at Y=" + to.getY() + "! Cancelling movement.");
            event.setCancelled(true);
        }
    }
}
