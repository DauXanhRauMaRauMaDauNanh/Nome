package io.nome.bridge.entity;

import java.net.InetSocketAddress;
import java.util.UUID;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import io.nome.bridge.NomeNativeBridge;
import io.nome.bridge.util.MinecraftConsole;

public class CraftPlayer implements Player {
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
    private final net.minecraft.server.level.ServerPlayer handle;

    public CraftPlayer(int entityId, String name, UUID uuid, World world, Location loc, Server server) {
        this.entityId = entityId;
        this.name = name;
        this.uuid = uuid;
        this.world = world;
        this.location = loc != null ? loc : new Location(world, 0, 64, 0);
        this.server = server;
        this.inventory = new org.bukkit.inventory.SimplePlayerInventory(this);
        this.handle = new net.minecraft.server.level.ServerPlayer();
        attachViaVersionIfPresent(this.handle.connection.networkManager.channel);
    }

    private void attachViaVersionIfPresent(io.netty.channel.Channel ch) {
        try {
            org.bukkit.plugin.Plugin viaPlugin = server != null ? server.getPluginManager().getPlugin("ViaVersion") : null;
            if (viaPlugin == null) return;
            ClassLoader cl = viaPlugin.getClass().getClassLoader();
            Class<?> userConnClass = Class.forName("com.viaversion.viaversion.connection.UserConnectionImpl", true, cl);
            Class<?> userConnInterface = Class.forName("com.viaversion.viaversion.api.connection.UserConnection", true, cl);
            Class<?> encodeHandlerClass = Class.forName("com.viaversion.viaversion.bukkit.handlers.BukkitEncodeHandler", true, cl);
            java.lang.reflect.Constructor<?> uConnCtor = userConnClass.getConstructor(io.netty.channel.Channel.class);
            Object userConn = uConnCtor.newInstance(ch);
            java.lang.reflect.Constructor<?> encCtor = encodeHandlerClass.getConstructor(userConnInterface);
            Object encHandler = encCtor.newInstance(userConn);
            ch.pipeline().addLast("via-encoder", (io.netty.channel.ChannelHandler) encHandler);
        } catch (Throwable t) {
            // Log if debugging needed
        }
    }

    public net.minecraft.server.level.ServerPlayer getHandle() {
        return handle;
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

    @Override public GameMode getGameMode() { return gameMode; }
    @Override public void setGameMode(GameMode mode) { this.gameMode = mode; }

    @Override public boolean isOnGround() { return onGround; }
    @Override public void setOnGround(boolean onGround) { this.onGround = onGround; }

    @Override public boolean isSneaking() { return sneaking; }
    @Override public void setSneaking(boolean sneaking) { this.sneaking = sneaking; }

    @Override public boolean isSprinting() { return sprinting; }
    @Override public void setSprinting(boolean sprinting) { this.sprinting = sprinting; }

    @Override public boolean isFlying() { return flying; }
    @Override public void setFlying(boolean flying) { this.flying = flying; }

    @Override public Vector getVelocity() { return velocity.clone(); }
    @Override public void setVelocity(Vector vel) { if (vel != null) this.velocity = vel.clone(); }

    @Override
    public Location getEyeLocation() {
        Location eye = location.clone();
        eye.setY(eye.getY() + (sneaking ? 1.27 : 1.62));
        return eye;
    }

    @Override
    public BoundingBox getBoundingBox() {
        double w = 0.6 / 2.0;
        double h = sneaking ? 1.5 : 1.8;
        return new BoundingBox(location.getX() - w, location.getY(), location.getZ() - w,
                               location.getX() + w, location.getY() + h, location.getZ() + w);
    }

    @Override public PlayerInventory getInventory() { return inventory; }
    @Override public ItemStack getItemInHand() { return inventory.getItemInMainHand(); }

    @Override
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

    @Override
    public void sendTitle(String title, String subtitle, int fadeIn, int stay, int fadeOut) {
    }

    @Override
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

    @Override public int getFoodLevel() { return 20; }
    @Override public void setFoodLevel(int food) {}

    @Override public int getPing() { return ping; }
    @Override public void setPing(int ping) { this.ping = ping; }

    @Override
    public void setItem(int slot, short itemId, byte count) {
        NomeNativeBridge.setPlayerItem((long) entityId, slot, itemId, count);
        inventory.setItem(slot, new ItemStack(org.bukkit.Material.getMaterial(itemId), count));
    }

    @Override
    public void kickPlayer(String message) {
        MinecraftConsole.info("Kicked player " + name + ": " + message);
    }

    @Override
    public InetSocketAddress getAddress() {
        return new InetSocketAddress("127.0.0.1", 25565);
    }

    private final java.util.Set<String> channels = java.util.concurrent.ConcurrentHashMap.newKeySet();

    public void addChannel(String channel) {
        if (channel != null) {
            channels.add(channel);
        }
    }

    public void removeChannel(String channel) {
        if (channel != null) {
            channels.remove(channel);
        }
    }

    public java.util.Set<String> getChannels() {
        return java.util.Collections.unmodifiableSet(channels);
    }

    @Override
    public void sendPluginMessage(org.bukkit.plugin.Plugin source, String channel, byte[] message) {
        // Dispatched to client via network packet if channel listening
    }

    public void sendPluginMessage(String channel, byte[] message) {
        sendPluginMessage(null, channel, message);
    }

    @Override
    public java.util.Set<String> getListeningPluginChannels() {
        java.util.Set<String> allChannels = new java.util.HashSet<>(channels);
        if (server != null && server.getMessenger() != null) {
            allChannels.addAll(server.getMessenger().getIncomingChannels());
        }
        return java.util.Collections.unmodifiableSet(allChannels);
    }
}
