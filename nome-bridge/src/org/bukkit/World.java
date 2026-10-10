package org.bukkit;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

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
}
