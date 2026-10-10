package org.bukkit.block;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import io.nome.bridge.NomeNativeBridge;

public class Block {
    private final World world;
    private final int x, y, z;

    public Block(World world, int x, int y, int z) {
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public World getWorld() { return world; }
    public int getX() { return x; }
    public int getY() { return y; }
    public int getZ() { return z; }

    public Location getLocation() {
        return new Location(world, x, y, z);
    }

    public Material getType() {
        short id = NomeNativeBridge.getBlock(x, y, z);
        return Material.getMaterial(id);
    }

    public void setType(Material material) {
        if (material == null) material = Material.AIR;
        NomeNativeBridge.setBlock(x, y, z, (short) material.getId());
    }

    public short getTypeId() {
        return NomeNativeBridge.getBlock(x, y, z);
    }

    public void setTypeId(short id) {
        NomeNativeBridge.setBlock(x, y, z, id);
    }
}
