package org.bukkit.craftbukkit.v1_21_R3.entity;

import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.World;

public class CraftPlayer extends io.nome.bridge.entity.CraftPlayer {
    public CraftPlayer(int entityId, String name, UUID uuid, World world, Location loc, Server server) {
        super(entityId, name, uuid, world, loc, server);
    }
}
