package org.joml;

public class Quaternionf {
    public float x;
    public float y;
    public float z;
    public float w;

    public Quaternionf() {
        this.w = 1.0f;
    }

    public Quaternionf(float x, float y, float z, float w) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.w = w;
    }

    public Quaternionf set(float x, float y, float z, float w) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.w = w;
        return this;
    }

    public Quaternionf identity() {
        this.x = 0;
        this.y = 0;
        this.z = 0;
        this.w = 1.0f;
        return this;
    }
}
