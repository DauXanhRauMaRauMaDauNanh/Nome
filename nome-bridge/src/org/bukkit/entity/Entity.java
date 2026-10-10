package org.bukkit.entity;

import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.World;

public interface Entity {
    int getEntityId();
    UUID getUniqueId();
    String getName();
    World getWorld();
    Location getLocation();
    void teleport(Location location);
    boolean isDead();
    void remove();
    EntityType getType();
}
