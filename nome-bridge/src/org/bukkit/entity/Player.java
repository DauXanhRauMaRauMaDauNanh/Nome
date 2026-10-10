package org.bukkit.entity;

import java.net.InetSocketAddress;
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import io.nome.bridge.NomeNativeBridge;

public class Player implements Entity, CommandSender {
    private final int entityId;
    private final String name;
    private final UUID uuid;
    private World world;
    private Location location;
    private double health = 20.0;
    private int foodLevel = 20;
    private int ping = 0;
    private boolean isOp = false;
    private final Server server;

    public Player(int entityId, String name, UUID uuid, World world, Location loc, Server server) {
        this.entityId = entityId;
        this.name = name;
        this.uuid = uuid;
        this.world = world;
        this.location = loc;
        this.server = server;
    }

    @Override public int getEntityId() { return entityId; }
    @Override public UUID getUniqueId() { return uuid; }
    @Override public String getName() { return name; }
    @Override public World getWorld() { return world; }
    @Override public Location getLocation() { return location.clone(); }
    @Override public boolean isDead() { return health <= 0; }
    @Override public void remove() {}
    @Override public EntityType getType() { return EntityType.PLAYER; }
    @Override public Server getServer() { return server; }
    @Override public boolean isOp() { return isOp; }
    @Override public void setOp(boolean op) { this.isOp = op; }
    @Override public boolean hasPermission(String perm) { return isOp; }

    @Override
    public void sendMessage(String message) {
        System.out.println("[Chat to " + name + "] " + message);
        NomeNativeBridge.broadcastMessage("§7[" + name + "]: " + message);
    }

    @Override
    public void sendMessage(String[] messages) {
        for (String msg : messages) sendMessage(msg);
    }

    @Override
    public void teleport(Location loc) {
        if (loc == null) return;
        this.world = loc.getWorld();
        this.location = loc.clone();
        NomeNativeBridge.teleportPlayer((long) entityId, loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch());
        System.out.println("[Nome-Server] Teleported player " + name + " to " + loc);
    }

    public double getHealth() { return health; }
    public void setHealth(double health) { this.health = health; }

    public int getFoodLevel() { return foodLevel; }
    public void setFoodLevel(int food) { this.foodLevel = food; }

    public int getPing() { return ping; }
    public void setPing(int ping) { this.ping = ping; }

    public void kickPlayer(String message) {
        System.out.println("[Nome-Server] Kicked player " + name + ": " + message);
    }

    public InetSocketAddress getAddress() {
        return new InetSocketAddress("127.0.0.1", 25565);
    }
}
