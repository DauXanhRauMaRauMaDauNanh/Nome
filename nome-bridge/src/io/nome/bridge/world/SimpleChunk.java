package io.nome.bridge.world;

import org.bukkit.Chunk;
import org.bukkit.ChunkSnapshot;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import io.nome.bridge.NomeNativeBridge;

public class SimpleChunk implements Chunk {
    private final World world;
    private final int x;
    private final int z;

    public SimpleChunk(World world, int x, int z) {
        this.world = world;
        this.x = x;
        this.z = z;
    }

    @Override public int getX() { return x; }
    @Override public int getZ() { return z; }
    @Override public World getWorld() { return world; }

    @Override
    public Block getBlock(int lx, int ly, int lz) {
        int gx = (x << 4) + (lx & 15);
        int gz = (z << 4) + (lz & 15);
        return world.getBlockAt(gx, ly, gz);
    }

    @Override
    public ChunkSnapshot getChunkSnapshot() {
        return getChunkSnapshot(false, false, false);
    }

    @Override
    public ChunkSnapshot getChunkSnapshot(boolean includeMaxbiome, boolean includeBiome, boolean includeBiomeTempRain) {
        short[] blocks = new short[16 * 16 * 384];
        NomeNativeBridge.getChunkAllBlocks(x, z, blocks);
        return new SimpleChunkSnapshot(x, z, world.getName(), blocks);
    }

    @Override
    public boolean isLoaded() {
        return NomeNativeBridge.isChunkLoaded(x, z);
    }

    @Override
    public boolean load() {
        return load(true);
    }

    @Override
    public boolean load(boolean generate) {
        NomeNativeBridge.loadChunk(x, z);
        return true;
    }

    @Override
    public boolean unload() {
        return unload(true);
    }

    @Override
    public boolean unload(boolean save) {
        return NomeNativeBridge.unloadChunk(x, z, false);
    }
}
