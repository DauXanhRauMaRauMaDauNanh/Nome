package io.netty.channel;

import io.netty.util.AttributeMap;
import java.net.SocketAddress;

public interface Channel extends AttributeMap, Comparable<Channel> {
    ChannelId id();
    EventLoop eventLoop();
    Channel parent();
    ChannelPipeline pipeline();

    boolean isOpen();
    boolean isRegistered();
    boolean isActive();
    boolean isWritable();

    SocketAddress localAddress();
    SocketAddress remoteAddress();

    ChannelFuture closeFuture();

    Channel read();
    Channel flush();

    ChannelFuture write(Object msg);
    ChannelFuture write(Object msg, ChannelPromise promise);
    ChannelFuture writeAndFlush(Object msg);
    ChannelFuture writeAndFlush(Object msg, ChannelPromise promise);
    ChannelPromise newPromise();
    ChannelFuture newSucceededFuture();
    ChannelFuture newFailedFuture(Throwable cause);

    ChannelFuture close();
    ChannelFuture close(ChannelPromise promise);
    ChannelFuture disconnect();
    ChannelFuture disconnect(ChannelPromise promise);
}
