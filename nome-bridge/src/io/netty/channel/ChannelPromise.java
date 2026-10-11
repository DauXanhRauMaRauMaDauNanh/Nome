package io.netty.channel;

import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;

public interface ChannelPromise extends ChannelFuture {
    ChannelPromise setSuccess(Void result);
    ChannelPromise setSuccess();
    boolean trySuccess();
    ChannelPromise setFailure(Throwable cause);
    boolean tryFailure(Throwable cause);
    @Override
    ChannelPromise addListener(GenericFutureListener<? extends Future<? super Void>> listener);
}
