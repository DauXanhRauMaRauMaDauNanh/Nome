package net.minecraft.network;

import io.netty.channel.Channel;
import io.netty.channel.embedded.EmbeddedChannel;

public class Connection {
    public Channel channel;

    public Connection() {
        this.channel = new EmbeddedChannel();
    }

    public Connection(Channel channel) {
        this.channel = channel != null ? channel : new EmbeddedChannel();
    }

    public Channel channel() {
        return channel;
    }

    public Channel getChannel() {
        return channel;
    }
}
