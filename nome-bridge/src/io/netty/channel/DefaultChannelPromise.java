package io.netty.channel;

import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import java.util.concurrent.TimeUnit;

public class DefaultChannelPromise implements ChannelPromise {
    private final Channel channel;
    private volatile boolean done = false;
    private volatile boolean success = false;
    private volatile Throwable cause = null;

    public DefaultChannelPromise(Channel channel) {
        this.channel = channel;
    }

    @Override public Channel channel() { return channel; }
    @Override public boolean isSuccess() { return success; }
    @Override public boolean isCancellable() { return false; }
    @Override public Throwable cause() { return cause; }
    @Override public boolean isDone() { return done; }
    @Override public boolean isCancelled() { return false; }
    @Override public boolean cancel(boolean mayInterruptIfRunning) { return false; }
    @Override public Void get() { return null; }
    @Override public Void get(long timeout, TimeUnit unit) { return null; }
    @Override public Void getNow() { return null; }

    @Override
    public ChannelPromise setSuccess(Void result) {
        return setSuccess();
    }

    @Override
    public ChannelPromise setSuccess() {
        this.success = true;
        this.done = true;
        return this;
    }

    @Override
    public boolean trySuccess() {
        if (!done) { setSuccess(); return true; }
        return false;
    }

    @Override
    public ChannelPromise setFailure(Throwable cause) {
        this.cause = cause;
        this.success = false;
        this.done = true;
        return this;
    }

    @Override
    public boolean tryFailure(Throwable cause) {
        if (!done) { setFailure(cause); return true; }
        return false;
    }

    @Override public ChannelPromise addListener(GenericFutureListener<? extends Future<? super Void>> listener) { return this; }
    @Override public ChannelFuture removeListener(GenericFutureListener<? extends Future<? super Void>> listener) { return this; }
    @Override public ChannelFuture sync() { return this; }
    @Override public ChannelFuture syncUninterruptibly() { return this; }
    @Override public ChannelFuture await() { return this; }
    @Override public ChannelFuture awaitUninterruptibly() { return this; }
    @Override public boolean await(long timeout, TimeUnit unit) { return true; }
    @Override public boolean await(long timeoutMillis) { return true; }
}
