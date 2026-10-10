package org.bukkit;

import java.util.UUID;
import org.bukkit.entity.Player;

public interface OfflinePlayer {
    boolean isOnline();
    String getName();
    UUID getUniqueId();
    boolean isOp();
    void setOp(boolean value);
    Player getPlayer();
    long getFirstPlayed();
    long getLastPlayed();
    boolean hasPlayedBefore();
}
