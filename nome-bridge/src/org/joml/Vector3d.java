package org.joml;

public class Vector3d {
    public double x;
    public double y;
    public double z;

    public Vector3d() {
        this(0, 0, 0);
    }

    public Vector3d(double d) {
        this(d, d, d);
    }

    public Vector3d(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public Vector3d(Vector3d v) {
        this(v.x, v.y, v.z);
    }

    public Vector3d set(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
        return this;
    }

    public Vector3d set(Vector3d v) {
        return set(v.x, v.y, v.z);
    }

    public Vector3d add(double x, double y, double z) {
        this.x += x;
        this.y += y;
        this.z += z;
        return this;
    }

    public Vector3d add(Vector3d v) {
        return add(v.x, v.y, v.z);
    }

    public Vector3d sub(double x, double y, double z) {
        this.x -= x;
        this.y -= y;
        this.z -= z;
        return this;
    }

    public Vector3d sub(Vector3d v) {
        return sub(v.x, v.y, v.z);
    }

    public Vector3d mul(double scalar) {
        this.x *= scalar;
        this.y *= scalar;
        this.z *= scalar;
        return this;
    }

    public double lengthSquared() {
        return x * x + y * y + z * z;
    }

    public double length() {
        return Math.sqrt(lengthSquared());
    }

    public Vector3d normalize() {
        double inv = 1.0 / length();
        this.x *= inv;
        this.y *= inv;
        this.z *= inv;
        return this;
    }

    @Override
    public String toString() {
        return "(" + x + ", " + y + ", " + z + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o instanceof Vector3d v) {
            return Double.doubleToLongBits(x) == Double.doubleToLongBits(v.x)
                && Double.doubleToLongBits(y) == Double.doubleToLongBits(v.y)
                && Double.doubleToLongBits(z) == Double.doubleToLongBits(v.z);
        }
        return false;
    }

    @Override
    public int hashCode() {
        long lx = Double.doubleToLongBits(x);
        long ly = Double.doubleToLongBits(y);
        long lz = Double.doubleToLongBits(z);
        int result = (int) (lx ^ (lx >>> 32));
        result = 31 * result + (int) (ly ^ (ly >>> 32));
        result = 31 * result + (int) (lz ^ (lz >>> 32));
        return result;
    }
}
