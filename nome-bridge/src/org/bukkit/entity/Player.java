package org.bukkit.entity;

import java.net.InetSocketAddress;
import java.util.UUID;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import io.nome.bridge.NomeNativeBridge;
import io.nome.bridge.util.MinecraftConsole;

public class Player implements Entity, CommandSender, OfflinePlayer {
    private final int entityId;
    private final String name;
    private final UUID uuid;
    private World world;
    private Location location;
    private int ping = 0;
    private boolean isOp = false;
    private final Server server;
    private GameMode gameMode = GameMode.SURVIVAL;
    private boolean onGround = true;
    private boolean sneaking = false;
    private boolean sprinting = false;
    private boolean flying = false;
    private Vector velocity = new Vector(0, 0, 0);
    private final PlayerInventory inventory;

    public Player(int entityId, String name, UUID uuid, World world, Location loc, Server server) {
        this.entityId = entityId;
        this.name = name;
        this.uuid = uuid;
        this.world = world;
        this.location = loc != null ? loc : new Location(world, 0, 64, 0);
        this.server = server;
        this.inventory = new org.bukkit.inventory.SimplePlayerInventory(this);
    }

    @Override public int getEntityId() { return entityId; }
    @Override public UUID getUniqueId() { return uuid; }
    @Override public String getName() { return name; }
    @Override public World getWorld() { return world; }
    @Override public Location getLocation() { return location.clone(); }
    @Override public boolean isDead() { return getHealth() <= 0; }
    @Override public void remove() {}
    @Override public EntityType getType() { return EntityType.PLAYER; }
    @Override public Server getServer() { return server; }
    @Override public boolean isOp() { return isOp; }
    @Override public void setOp(boolean op) { this.isOp = op; }
    @Override public boolean hasPermission(String perm) { return isOp; }

    @Override public boolean isOnline() { return true; }
    @Override public Player getPlayer() { return this; }
    @Override public long getFirstPlayed() { return System.currentTimeMillis(); }
    @Override public long getLastPlayed() { return System.currentTimeMillis(); }
    @Override public boolean hasPlayedBefore() { return true; }

    public GameMode getGameMode() { return gameMode; }
    public void setGameMode(GameMode mode) { this.gameMode = mode; }

    public boolean isOnGround() { return onGround; }
    public void setOnGround(boolean onGround) { this.onGround = onGround; }

    public boolean isSneaking() { return sneaking; }
    public void setSneaking(boolean sneaking) { this.sneaking = sneaking; }

    public boolean isSprinting() { return sprinting; }
    public void setSprinting(boolean sprinting) { this.sprinting = sprinting; }

    public boolean isFlying() { return flying; }
    public void setFlying(boolean flying) { this.flying = flying; }

    public Vector getVelocity() { return velocity.clone(); }
    public void setVelocity(Vector vel) { if (vel != null) this.velocity = vel.clone(); }

    public Location getEyeLocation() {
        Location eye = location.clone();
        eye.setY(eye.getY() + (sneaking ? 1.27 : 1.62));
        return eye;
    }

    public BoundingBox getBoundingBox() {
        double w = 0.6 / 2.0;
        double h = sneaking ? 1.5 : 1.8;
        return new BoundingBox(location.getX() - w, location.getY(), location.getZ() - w,
                               location.getX() + w, location.getY() + h, location.getZ() + w);
    }

    public PlayerInventory getInventory() { return inventory; }
    public ItemStack getItemInHand() { return inventory.getItemInMainHand(); }

    public void setLocationInternal(Location loc) {
        if (loc != null) {
            this.location = loc.clone();
            this.world = loc.getWorld();
        }
    }

    @Override
    public void sendMessage(String message) {
        NomeNativeBridge.broadcastMessage("§7[" + name + "]: " + message);
    }

    @Override
    public void sendMessage(String[] messages) {
        for (String msg : messages) sendMessage(msg);
    }

    public void sendTitle(String title, String subtitle, int fadeIn, int stay, int fadeOut) {
    }

    public void sendActionBar(String message) {
    }

    @Override
    public void teleport(Location loc) {
        if (loc == null) return;
        this.world = loc.getWorld();
        this.location = loc.clone();
        NomeNativeBridge.teleportPlayer((long) entityId, loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch());
        MinecraftConsole.info("Teleported player " + name + " to " + loc);
    }

    // Direct Off-Heap Native Access (Zero GC Allocations)
    public double getHealth() { 
        return (double) NomeNativeBridge.getPlayerHealth((long) entityId); 
    }

    public void setHealth(double health) { 
        NomeNativeBridge.setPlayerHealth((long) entityId, (int) health); 
    }

    public int getFoodLevel() { return 20; }
    public void setFoodLevel(int food) {}

    public int getPing() { return ping; }
    public void setPing(int ping) { this.ping = ping; }

    public void setItem(int slot, short itemId, byte count) {
        NomeNativeBridge.setPlayerItem((long) entityId, slot, itemId, count);
        inventory.setItem(slot, new ItemStack(org.bukkit.Material.getMaterial(itemId), count));
    }

    public void kickPlayer(String message) {
        MinecraftConsole.info("Kicked player " + name + ": " + message);
    }

    public InetSocketAddress getAddress() {
        return new InetSocketAddress("127.0.0.1", 25565);
    }
}
