package org.bukkit;

/**
 * Represents a static snapshot of a Chunk's block data.
 */
public interface ChunkSnapshot {
    int getX();
    int getZ();
    String getWorldName();
    Material getBlockType(int x, int y, int z);
    int getData(int x, int y, int z);
    int getHighestBlockYAt(int x, int z);
    long getCaptureFullTime();
    boolean isSectionEmpty(int sy);
}
