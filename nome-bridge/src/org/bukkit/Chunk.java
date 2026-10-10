package org.bukkit;

import org.bukkit.block.Block;

/**
 * Represents a 16x16 chunk of blocks in a Minecraft World.
 */
public interface Chunk {
    int getX();
    int getZ();
    World getWorld();
    Block getBlock(int x, int y, int z);
    ChunkSnapshot getChunkSnapshot();
    ChunkSnapshot getChunkSnapshot(boolean includeMaxbiome, boolean includeBiome, boolean includeBiomeTempRain);
    boolean isLoaded();
    boolean load();
    boolean load(boolean generate);
    boolean unload();
    boolean unload(boolean save);
}
