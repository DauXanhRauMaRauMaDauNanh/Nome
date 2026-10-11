package io.netty.channel;

import java.util.List;
import java.util.Map;

public interface ChannelPipeline extends Iterable<Map.Entry<String, ChannelHandler>> {
    ChannelPipeline addFirst(String name, ChannelHandler handler);
    ChannelPipeline addLast(String name, ChannelHandler handler);
    ChannelPipeline addBefore(String baseName, String name, ChannelHandler handler);
    ChannelPipeline addAfter(String baseName, String name, ChannelHandler handler);
    ChannelPipeline remove(ChannelHandler handler);
    ChannelHandler remove(String name);
    <T extends ChannelHandler> T remove(Class<T> handlerType);

    ChannelHandler first();
    ChannelHandlerContext firstContext();
    ChannelHandler last();
    ChannelHandlerContext lastContext();
    ChannelHandler get(String name);
    <T extends ChannelHandler> T get(Class<T> handlerType);
    ChannelHandlerContext context(String name);
    ChannelHandlerContext context(ChannelHandler handler);
    ChannelHandlerContext context(Class<? extends ChannelHandler> handlerType);

    Channel channel();
    List<String> names();
    Map<String, ChannelHandler> toMap();

    ChannelPipeline fireChannelRegistered();
    ChannelPipeline fireChannelActive();
    ChannelPipeline fireChannelInactive();
    ChannelPipeline fireExceptionCaught(Throwable cause);
    ChannelPipeline fireChannelRead(Object msg);
    ChannelPipeline fireChannelReadComplete();

    ChannelFuture write(Object msg);
    ChannelFuture write(Object msg, ChannelPromise promise);
    ChannelPipeline flush();
    ChannelFuture writeAndFlush(Object msg);
    ChannelFuture writeAndFlush(Object msg, ChannelPromise promise);
    ChannelFuture close();
    ChannelFuture close(ChannelPromise promise);
}
