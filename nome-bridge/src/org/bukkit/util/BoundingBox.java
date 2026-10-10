package org.bukkit.util;

public class BoundingBox implements Cloneable {
    private double minX, minY, minZ;
    private double maxX, maxY, maxZ;

    public BoundingBox(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        this.minX = Math.min(minX, maxX);
        this.minY = Math.min(minY, maxY);
        this.minZ = Math.min(minZ, maxZ);
        this.maxX = Math.max(minX, maxX);
        this.maxY = Math.max(minY, maxY);
        this.maxZ = Math.max(minZ, maxZ);
    }

    public static BoundingBox of(Vector min, Vector max) {
        return new BoundingBox(min.getX(), min.getY(), min.getZ(), max.getX(), max.getY(), max.getZ());
    }

    public double getMinX() { return minX; }
    public double getMinY() { return minY; }
    public double getMinZ() { return minZ; }
    public double getMaxX() { return maxX; }
    public double getMaxY() { return maxY; }
    public double getMaxZ() { return maxZ; }

    public double getWidthX() { return maxX - minX; }
    public double getHeight() { return maxY - minY; }
    public double getWidthZ() { return maxZ - minZ; }

    public boolean overlaps(BoundingBox other) {
        return this.minX < other.maxX && this.maxX > other.minX
            && this.minY < other.maxY && this.maxY > other.minY
            && this.minZ < other.maxZ && this.maxZ > other.minZ;
    }

    public boolean contains(double x, double y, double z) {
        return x >= this.minX && x <= this.maxX
            && y >= this.minY && y <= this.maxY
            && z >= this.minZ && z <= this.maxZ;
    }

    public boolean contains(Vector position) {
        return contains(position.getX(), position.getY(), position.getZ());
    }

    @Override
    public BoundingBox clone() {
        return new BoundingBox(minX, minY, minZ, maxX, maxY, maxZ);
    }
}
