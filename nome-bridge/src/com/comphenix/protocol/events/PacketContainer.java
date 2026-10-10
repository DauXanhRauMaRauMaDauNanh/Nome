package com.comphenix.protocol.events;

import java.util.Arrays;

public class PacketContainer {
    private final int type;
    private byte[] data;

    public PacketContainer(int type, byte[] data) {
        this.type = type;
        this.data = data != null ? data : new byte[0];
    }

    public int getType() {
        return type;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data != null ? data : new byte[0];
    }

    public int getByteSize() {
        return data.length;
    }

    @Override
    public String toString() {
        return "PacketContainer{type=0x" + Integer.toHexString(type) + ", len=" + data.length + "}";
    }
}
