package io.netty.channel;

import io.netty.util.concurrent.GenericFutureListener;

public interface ChannelFutureListener extends GenericFutureListener<ChannelFuture> {
    ChannelFutureListener CLOSE = future -> future.channel().close();
    ChannelFutureListener FIRE_EXCEPTION_ON_FAILURE = future -> {
        if (!future.isSuccess()) {
            future.channel().pipeline().fireExceptionCaught(future.cause());
        }
    };
}
