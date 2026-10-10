package org.bukkit;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import io.nome.bridge.NomeNativeBridge;

public class World {
    private final String name;
    private final UUID uuid;
    private final List<Player> players = new ArrayList<>();
    private final List<Entity> entities = new ArrayList<>();
    private long time = 0;

    public World(String name) {
        this.name = name;
        this.uuid = UUID.randomUUID();
    }

    public String getName() { return name; }
    public UUID getUID() { return uuid; }

    public Block getBlockAt(int x, int y, int z) {
        return new Block(this, x, y, z);
    }

    public Block getBlockAt(Location loc) {
        return getBlockAt(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
    }

    public List<Player> getPlayers() {
        return new ArrayList<>(players);
    }

    public List<Entity> getEntities() {
        return new ArrayList<>(entities);
    }

    public void addPlayer(Player p) {
        players.add(p);
        entities.add(p);
    }

    public void removePlayer(Player p) {
        players.remove(p);
        entities.remove(p);
    }

    public long getTime() { return time; }
    public void setTime(long time) { this.time = time; }

    // 100% Native C++ Entity Spawn & ECS Management
    public int spawnEntity(Location loc, EntityType type) {
        if (loc == null || type == null) return -1;
        return NomeNativeBridge.spawnEntity((short) type.getTypeId(), loc.getX(), loc.getY(), loc.getZ());
    }

    // 100% Native Procedural Structure Locator
    public Location locateNearestStructure(String structureName, Location loc) {
        if (loc == null || structureName == null) return null;
        int chunkX = loc.getBlockX() >> 4;
        int chunkZ = loc.getBlockZ() >> 4;
        int[] result = NomeNativeBridge.locateStructure(structureName.toLowerCase(), chunkX, chunkZ);
        if (result != null) {
            return new Location(this, (result[0] << 4) + 8, 64, (result[1] << 4) + 8);
        }
        return null;
    }
}
