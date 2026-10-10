package org.bukkit;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import io.nome.bridge.NomeNativeBridge;
import io.nome.bridge.world.SimpleChunk;

public class World {
    private final String name;
    private final UUID uuid;
    private final List<Player> players = new ArrayList<>();
    private final List<Entity> entities = new ArrayList<>();
    private final Map<Long, Chunk> chunkCache = new ConcurrentHashMap<>();
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

    // Chunk System (Smart Chunk Access)
    private long getChunkKey(int cx, int cz) {
        return (((long) cx) << 32) | (cz & 0xFFFFFFFFL);
    }

    public Chunk getChunkAt(int cx, int cz) {
        return chunkCache.computeIfAbsent(getChunkKey(cx, cz), k -> new SimpleChunk(this, cx, cz));
    }

    public Chunk getChunkAt(Location loc) {
        return getChunkAt(loc.getBlockX() >> 4, loc.getBlockZ() >> 4);
    }

    public Chunk getChunkAt(Block block) {
        return getChunkAt(block.getX() >> 4, block.getZ() >> 4);
    }

    public Chunk[] getLoadedChunks() {
        return chunkCache.values().toArray(new Chunk[0]);
    }

    public boolean isChunkLoaded(int cx, int cz) {
        return NomeNativeBridge.isChunkLoaded(cx, cz);
    }

    public boolean isChunkLoaded(Chunk chunk) {
        return chunk != null && isChunkLoaded(chunk.getX(), chunk.getZ());
    }

    public boolean loadChunk(int cx, int cz) {
        return loadChunk(cx, cz, true);
    }

    public boolean loadChunk(int cx, int cz, boolean generate) {
        NomeNativeBridge.loadChunk(cx, cz);
        getChunkAt(cx, cz);
        return true;
    }

    public boolean unloadChunk(int cx, int cz) {
        return unloadChunk(cx, cz, true);
    }

    public boolean unloadChunk(int cx, int cz, boolean save) {
        chunkCache.remove(getChunkKey(cx, cz));
        return NomeNativeBridge.unloadChunk(cx, cz, false);
    }

    public boolean unloadChunk(Chunk chunk) {
        return chunk != null && unloadChunk(chunk.getX(), chunk.getZ());
    }

    public int getHighestBlockYAt(int x, int z) {
        int cx = x >> 4;
        int cz = z >> 4;
        int lx = x & 15;
        int lz = z & 15;
        short[] heightmap = new short[256];
        NomeNativeBridge.getChunkHeightmap(cx, cz, heightmap);
        return heightmap[lz * 16 + lx];
    }

    public Block getHighestBlockAt(int x, int z) {
        return getBlockAt(x, getHighestBlockYAt(x, z), z);
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
