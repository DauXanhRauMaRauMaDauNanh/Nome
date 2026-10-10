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

public interface Player extends Entity, CommandSender, OfflinePlayer, org.bukkit.plugin.messaging.PluginMessageRecipient {
    GameMode getGameMode();
    void setGameMode(GameMode mode);
    boolean isOnGround();
    void setOnGround(boolean onGround);
    boolean isSneaking();
    void setSneaking(boolean sneaking);
    boolean isSprinting();
    void setSprinting(boolean sprinting);
    boolean isFlying();
    void setFlying(boolean flying);
    Vector getVelocity();
    void setVelocity(Vector vel);
    Location getEyeLocation();
    BoundingBox getBoundingBox();
    PlayerInventory getInventory();
    ItemStack getItemInHand();
    void sendTitle(String title, String subtitle, int fadeIn, int stay, int fadeOut);
    void sendActionBar(String message);
    int getFoodLevel();
    void setFoodLevel(int food);
    int getPing();
    void setPing(int ping);
    void setItem(int slot, short itemId, byte count);
    void kickPlayer(String message);
    InetSocketAddress getAddress();
    void setLocationInternal(Location loc);
}
