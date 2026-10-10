package com.comphenix.protocol;

/**
 * Drop-in ProtocolLibrary singleton for Anticheat (GrimAC, Vulcan, Matrix)
 * and Packet-level plugins (RayTraceAntiXray, Orebfuscator).
 */
public final class ProtocolLibrary {

    private static final ProtocolManager manager = new ProtocolManager();

    private ProtocolLibrary() {}

    public static ProtocolManager getProtocolManager() {
        return manager;
    }
}
