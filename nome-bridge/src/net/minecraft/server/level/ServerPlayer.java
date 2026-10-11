package net.minecraft.server.level;

import net.minecraft.server.network.ServerGamePacketListenerImpl;

public class ServerPlayer {
    public ServerGamePacketListenerImpl connection;
    public ServerGamePacketListenerImpl playerConnection;

    public ServerPlayer() {
        this.connection = new ServerGamePacketListenerImpl();
        this.playerConnection = this.connection;
    }

    public ServerPlayer(ServerGamePacketListenerImpl connection) {
        this.connection = connection != null ? connection : new ServerGamePacketListenerImpl();
        this.playerConnection = this.connection;
    }
}
