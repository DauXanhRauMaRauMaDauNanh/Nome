package org.bukkit.event.player;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;

public class PlayerReceivePacketEvent extends PlayerEvent implements Cancellable {
    private final int packetId;
    private byte[] payload;
    private boolean cancelled = false;

    public PlayerReceivePacketEvent(Player player, int packetId, byte[] payload) {
        super(player);
        this.packetId = packetId;
        this.payload = payload != null ? payload : new byte[0];
    }

    public int getPacketId() {
        return packetId;
    }

    public byte[] getPayload() {
        return payload;
    }

    public void setPayload(byte[] payload) {
        this.payload = payload != null ? payload : new byte[0];
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
