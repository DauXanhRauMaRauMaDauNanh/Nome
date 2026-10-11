package io.netty.channel;

import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import io.netty.util.AttributeMap;

public interface ChannelHandlerContext extends AttributeMap {
    Channel channel();
    ChannelPipeline pipeline();
    String name();
    ChannelHandler handler();

    ChannelHandlerContext fireChannelRegistered();
    ChannelHandlerContext fireChannelActive();
    ChannelHandlerContext fireChannelInactive();
    ChannelHandlerContext fireExceptionCaught(Throwable cause);
    ChannelHandlerContext fireChannelRead(Object msg);
    ChannelHandlerContext fireChannelReadComplete();

    ChannelFuture write(Object msg);
    ChannelFuture write(Object msg, ChannelPromise promise);
    ChannelHandlerContext flush();
    ChannelFuture writeAndFlush(Object msg);
    ChannelFuture writeAndFlush(Object msg, ChannelPromise promise);
    ChannelFuture close();
    ChannelFuture close(ChannelPromise promise);

    ChannelFuture bind(java.net.SocketAddress localAddress, ChannelPromise promise);
    ChannelFuture connect(java.net.SocketAddress remoteAddress, java.net.SocketAddress localAddress, ChannelPromise promise);
    ChannelFuture disconnect(ChannelPromise promise);
    ChannelFuture deregister(ChannelPromise promise);
    ChannelHandlerContext read();

    @Override
    default <T> Attribute<T> attr(AttributeKey<T> key) {
        return channel().attr(key);
    }

    @Override
    default <T> boolean hasAttr(AttributeKey<T> key) {
        return channel().hasAttr(key);
    }
}
