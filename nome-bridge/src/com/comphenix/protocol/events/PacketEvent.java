package com.comphenix.protocol.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;

public class PacketEvent implements Cancellable {
    private final Player player;
    private final PacketContainer packet;
    private boolean cancelled = false;

    public PacketEvent(Player player, PacketContainer packet) {
        this.player = player;
        this.packet = packet;
    }

    public Player getPlayer() {
        return player;
    }

    public PacketContainer getPacket() {
        return packet;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }
}
