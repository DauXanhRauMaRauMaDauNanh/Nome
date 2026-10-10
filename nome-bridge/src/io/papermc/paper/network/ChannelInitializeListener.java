package io.papermc.paper.network;

import io.netty.channel.Channel;

@FunctionalInterface
public interface ChannelInitializeListener {
    void afterInitChannel(Channel channel);
}
