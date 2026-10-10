package org.bukkit.util;

import java.io.Serializable;

public class Vector implements Cloneable, Serializable {
    private static final long serialVersionUID = -2657651106846175874L;

    protected double x;
    protected double y;
    protected double z;

    public Vector() {
        this(0, 0, 0);
    }

    public Vector(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public Vector(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public Vector(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public Vector add(Vector vec) {
        this.x += vec.x;
        this.y += vec.y;
        this.z += vec.z;
        return this;
    }

    public Vector subtract(Vector vec) {
        this.x -= vec.x;
        this.y -= vec.y;
        this.z -= vec.z;
        return this;
    }

    public Vector multiply(double m) {
        this.x *= m;
        this.y *= m;
        this.z *= m;
        return this;
    }

    public Vector divide(double m) {
        this.x /= m;
        this.y /= m;
        this.z /= m;
        return this;
    }

    public double length() {
        return Math.sqrt(x * x + y * y + z * z);
    }

    public double lengthSquared() {
        return x * x + y * y + z * z;
    }

    public double distance(Vector o) {
        return Math.sqrt(Math.pow(x - o.x, 2) + Math.pow(y - o.y, 2) + Math.pow(z - o.z, 2));
    }

    public double distanceSquared(Vector o) {
        return Math.pow(x - o.x, 2) + Math.pow(y - o.y, 2) + Math.pow(z - o.z, 2);
    }

    public Vector normalize() {
        double len = length();
        if (len != 0) {
            x /= len;
            y /= len;
            z /= len;
        }
        return this;
    }

    public Vector zero() {
        this.x = 0;
        this.y = 0;
        this.z = 0;
        return this;
    }

    public double getX() { return x; }
    public double getY() { return y; }
    public double getZ() { return z; }
    public int getBlockX() { return (int) Math.floor(x); }
    public int getBlockY() { return (int) Math.floor(y); }
    public int getBlockZ() { return (int) Math.floor(z); }

    public Vector setX(double x) { this.x = x; return this; }
    public Vector setY(double y) { this.y = y; return this; }
    public Vector setZ(double z) { this.z = z; return this; }

    @Override
    public Vector clone() {
        try {
            return (Vector) super.clone();
        } catch (CloneNotSupportedException e) {
            return new Vector(x, y, z);
        }
    }

    @Override
    public String toString() {
        return x + "," + y + "," + z;
    }
}
