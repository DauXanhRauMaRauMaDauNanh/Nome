package com.comphenix.protocol.events;

import org.bukkit.plugin.Plugin;
import java.util.HashSet;
import java.util.Set;

public abstract class PacketAdapter {
    private final Plugin plugin;
    private final Set<Integer> packetTypes = new HashSet<>();

    public PacketAdapter(Plugin plugin, int... types) {
        this.plugin = plugin;
        if (types != null) {
            for (int t : types) {
                packetTypes.add(t);
            }
        }
    }

    public Plugin getPlugin() {
        return plugin;
    }

    public boolean listensTo(int type) {
        return packetTypes.isEmpty() || packetTypes.contains(type);
    }

    public void onPacketReceiving(PacketEvent event) {}

    public void onPacketSending(PacketEvent event) {}
}
