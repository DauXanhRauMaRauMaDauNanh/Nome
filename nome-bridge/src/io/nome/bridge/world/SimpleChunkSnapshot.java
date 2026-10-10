package io.nome.bridge.world;

import org.bukkit.ChunkSnapshot;
import org.bukkit.Material;

public class SimpleChunkSnapshot implements ChunkSnapshot {
    private final int x;
    private final int z;
    private final String worldName;
    private final short[] blocks;
    private final long captureTime;

    public SimpleChunkSnapshot(int x, int z, String worldName, short[] blocks) {
        this.x = x;
        this.z = z;
        this.worldName = worldName;
        this.blocks = blocks != null ? blocks : new short[16 * 16 * 384];
        this.captureTime = System.currentTimeMillis();
    }

    @Override public int getX() { return x; }
    @Override public int getZ() { return z; }
    @Override public String getWorldName() { return worldName; }

    @Override
    public Material getBlockType(int lx, int ly, int lz) {
        if (lx < 0 || lx >= 16 || lz < 0 || lz >= 16 || ly < -64 || ly >= 320) {
            return Material.AIR;
        }
        int idx = ((ly + 64) * 256) + (lz * 16) + lx;
        if (idx >= 0 && idx < blocks.length) {
            return Material.getMaterial((int) blocks[idx]);
        }
        return Material.AIR;
    }

    @Override
    public int getData(int x, int y, int z) {
        return 0;
    }

    @Override
    public int getHighestBlockYAt(int lx, int lz) {
        for (int y = 319; y >= -64; y--) {
            if (getBlockType(lx, y, lz) != Material.AIR) {
                return y;
            }
        }
        return -64;
    }

    @Override
    public long getCaptureFullTime() {
        return captureTime;
    }

    @Override
    public boolean isSectionEmpty(int sy) {
        int baseIdx = sy * 4096;
        for (int i = 0; i < 4096; i++) {
            if (baseIdx + i < blocks.length && blocks[baseIdx + i] != 0) {
                return false;
            }
        }
        return true;
    }
}
