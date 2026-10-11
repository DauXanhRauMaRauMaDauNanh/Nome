package io.netty.channel.embedded;

import io.netty.channel.*;
import io.netty.util.DefaultAttributeMap;
import java.net.InetSocketAddress;
import java.net.SocketAddress;

public class EmbeddedChannel extends DefaultAttributeMap implements Channel {
    private final ChannelId id = new ChannelId() {
        @Override public String asShortText() { return "nome0"; }
        @Override public String asLongText() { return "nome-channel-0"; }
        @Override public int compareTo(ChannelId o) { return asLongText().compareTo(o.asLongText()); }
    };
    private final SocketAddress localAddress = new InetSocketAddress("127.0.0.1", 25565);
    private final SocketAddress remoteAddress = new InetSocketAddress("127.0.0.1", 54321);
    private boolean open = true;
    private boolean active = true;
    private final ChannelPipeline pipeline;

    public EmbeddedChannel() {
        this.pipeline = new DefaultChannelPipeline(this);
    }

    public EmbeddedChannel(ChannelHandler... handlers) {
        this();
        for (int i = 0; i < handlers.length; i++) {
            pipeline.addLast("handler-" + i, handlers[i]);
        }
    }

    @Override public ChannelId id() { return id; }
    @Override public EventLoop eventLoop() { return null; }
    @Override public Channel parent() { return null; }
    @Override public ChannelPipeline pipeline() { return pipeline; }

    @Override public boolean isOpen() { return open; }
    @Override public boolean isRegistered() { return true; }
    @Override public boolean isActive() { return active; }
    @Override public boolean isWritable() { return true; }

    @Override public SocketAddress localAddress() { return localAddress; }
    @Override public SocketAddress remoteAddress() { return remoteAddress; }

    @Override public ChannelFuture closeFuture() { return newSucceededFuture(); }
    @Override public Channel read() { return this; }
    @Override public Channel flush() { return this; }

    @Override public ChannelFuture write(Object msg) { return newSucceededFuture(); }
    @Override public ChannelFuture write(Object msg, ChannelPromise promise) { promise.setSuccess(); return promise; }
    @Override public ChannelFuture writeAndFlush(Object msg) { return newSucceededFuture(); }
    @Override public ChannelFuture writeAndFlush(Object msg, ChannelPromise promise) { promise.setSuccess(); return promise; }

    @Override public ChannelPromise newPromise() { return new DefaultChannelPromise(this); }
    @Override public ChannelFuture newSucceededFuture() {
        ChannelPromise p = newPromise();
        p.setSuccess();
        return p;
    }
    @Override public ChannelFuture newFailedFuture(Throwable cause) {
        ChannelPromise p = newPromise();
        p.setFailure(cause);
        return p;
    }

    @Override public ChannelFuture close() { this.open = false; this.active = false; return newSucceededFuture(); }
    @Override public ChannelFuture close(ChannelPromise promise) { this.open = false; this.active = false; promise.setSuccess(); return promise; }
    @Override public ChannelFuture disconnect() { return close(); }
    @Override public ChannelFuture disconnect(ChannelPromise promise) { return close(promise); }

    @Override
    public int compareTo(Channel o) {
        return id().asLongText().compareTo(o.id().asLongText());
    }
}
