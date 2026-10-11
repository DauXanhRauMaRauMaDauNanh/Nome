package net.minecraft.server.network;

import net.minecraft.network.Connection;

public class ServerGamePacketListenerImpl {
    public Connection connection;
    public Connection networkManager;

    public ServerGamePacketListenerImpl() {
        this.connection = new Connection();
        this.networkManager = this.connection;
    }

    public ServerGamePacketListenerImpl(Connection connection) {
        this.connection = connection != null ? connection : new Connection();
        this.networkManager = this.connection;
    }

    public Connection getConnection() {
        return connection;
    }
}
