package io.netty.channel;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class DefaultChannelPipeline implements ChannelPipeline {
    private final Channel channel;
    private final Map<String, ChannelHandler> handlers = new LinkedHashMap<>();

    public DefaultChannelPipeline(Channel channel) {
        this.channel = channel;
    }

    @Override
    public synchronized ChannelPipeline addFirst(String name, ChannelHandler handler) {
        Map<String, ChannelHandler> newMap = new LinkedHashMap<>();
        newMap.put(name, handler);
        newMap.putAll(handlers);
        handlers.clear();
        handlers.putAll(newMap);
        return this;
    }

    @Override
    public synchronized ChannelPipeline addLast(String name, ChannelHandler handler) {
        handlers.put(name, handler);
        return this;
    }

    @Override
    public synchronized ChannelPipeline addBefore(String baseName, String name, ChannelHandler handler) {
        Map<String, ChannelHandler> newMap = new LinkedHashMap<>();
        for (Map.Entry<String, ChannelHandler> entry : handlers.entrySet()) {
            if (entry.getKey().equals(baseName)) {
                newMap.put(name, handler);
            }
            newMap.put(entry.getKey(), entry.getValue());
        }
        handlers.clear();
        handlers.putAll(newMap);
        return this;
    }

    @Override
    public synchronized ChannelPipeline addAfter(String baseName, String name, ChannelHandler handler) {
        Map<String, ChannelHandler> newMap = new LinkedHashMap<>();
        for (Map.Entry<String, ChannelHandler> entry : handlers.entrySet()) {
            newMap.put(entry.getKey(), entry.getValue());
            if (entry.getKey().equals(baseName)) {
                newMap.put(name, handler);
            }
        }
        handlers.clear();
        handlers.putAll(newMap);
        return this;
    }

    @Override
    public synchronized ChannelPipeline remove(ChannelHandler handler) {
        handlers.values().remove(handler);
        return this;
    }

    @Override
    public synchronized ChannelHandler remove(String name) {
        return handlers.remove(name);
    }

    @Override
    @SuppressWarnings("unchecked")
    public synchronized <T extends ChannelHandler> T remove(Class<T> handlerType) {
        Iterator<Map.Entry<String, ChannelHandler>> it = handlers.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, ChannelHandler> entry = it.next();
            if (handlerType.isInstance(entry.getValue())) {
                it.remove();
                return (T) entry.getValue();
            }
        }
        return null;
    }

    @Override
    public synchronized ChannelHandler first() {
        return handlers.isEmpty() ? null : handlers.values().iterator().next();
    }

    @Override
    public synchronized ChannelHandlerContext firstContext() {
        ChannelHandler h = first();
        return h != null ? context(h) : null;
    }

    @Override
    public synchronized ChannelHandler last() {
        ChannelHandler last = null;
        for (ChannelHandler h : handlers.values()) last = h;
        return last;
    }

    @Override
    public synchronized ChannelHandlerContext lastContext() {
        ChannelHandler h = last();
        return h != null ? context(h) : null;
    }

    @Override
    public synchronized ChannelHandler get(String name) {
        return handlers.get(name);
    }

    @Override
    @SuppressWarnings("unchecked")
    public synchronized <T extends ChannelHandler> T get(Class<T> handlerType) {
        for (ChannelHandler h : handlers.values()) {
            if (handlerType.isInstance(h)) return (T) h;
        }
        return null;
    }

    @Override
    public synchronized ChannelHandlerContext context(String name) {
        ChannelHandler h = get(name);
        return h != null ? context(h) : null;
    }

    @Override
    public ChannelHandlerContext context(ChannelHandler handler) {
        return new DummyChannelHandlerContext(channel, this, handler);
    }

    @Override
    public ChannelHandlerContext context(Class<? extends ChannelHandler> handlerType) {
        ChannelHandler h = get(handlerType);
        return h != null ? context(h) : null;
    }

    @Override public Channel channel() { return channel; }
    @Override public synchronized List<String> names() { return new ArrayList<>(handlers.keySet()); }
    @Override public synchronized Map<String, ChannelHandler> toMap() { return new LinkedHashMap<>(handlers); }

    @Override public ChannelPipeline fireChannelRegistered() { return this; }
    @Override public ChannelPipeline fireChannelActive() { return this; }
    @Override public ChannelPipeline fireChannelInactive() { return this; }
    @Override public ChannelPipeline fireExceptionCaught(Throwable cause) { return this; }
    @Override public ChannelPipeline fireChannelRead(Object msg) { return this; }
    @Override public ChannelPipeline fireChannelReadComplete() { return this; }

    @Override public ChannelFuture write(Object msg) { return channel.newSucceededFuture(); }
    @Override public ChannelFuture write(Object msg, ChannelPromise promise) { promise.setSuccess(); return promise; }
    @Override public ChannelPipeline flush() { return this; }
    @Override public ChannelFuture writeAndFlush(Object msg) { return channel.newSucceededFuture(); }
    @Override public ChannelFuture writeAndFlush(Object msg, ChannelPromise promise) { promise.setSuccess(); return promise; }
    @Override public ChannelFuture close() { return channel.close(); }
    @Override public ChannelFuture close(ChannelPromise promise) { return channel.close(promise); }

    @Override
    public synchronized Iterator<Map.Entry<String, ChannelHandler>> iterator() {
        return new LinkedHashMap<>(handlers).entrySet().iterator();
    }

    private static class DummyChannelHandlerContext implements ChannelHandlerContext {
        private final Channel channel;
        private final ChannelPipeline pipeline;
        private final ChannelHandler handler;

        DummyChannelHandlerContext(Channel channel, ChannelPipeline pipeline, ChannelHandler handler) {
            this.channel = channel;
            this.pipeline = pipeline;
            this.handler = handler;
        }

        @Override public Channel channel() { return channel; }
        @Override public ChannelPipeline pipeline() { return pipeline; }
        @Override public ChannelHandler handler() { return handler; }
        @Override public String name() { return handler.getClass().getSimpleName(); }
        @Override public ChannelFuture write(Object msg) { return channel.newSucceededFuture(); }
        @Override public ChannelFuture write(Object msg, ChannelPromise promise) { promise.setSuccess(); return promise; }
        @Override public ChannelHandlerContext flush() { return this; }
        @Override public ChannelFuture writeAndFlush(Object msg) { return channel.newSucceededFuture(); }
        @Override public ChannelFuture writeAndFlush(Object msg, ChannelPromise promise) { promise.setSuccess(); return promise; }
        @Override public ChannelFuture close() { return channel.close(); }
        @Override public ChannelFuture close(ChannelPromise promise) { return channel.close(promise); }
        @Override public ChannelHandlerContext fireChannelRegistered() { return this; }
        @Override public ChannelHandlerContext fireChannelActive() { return this; }
        @Override public ChannelHandlerContext fireChannelInactive() { return this; }
        @Override public ChannelHandlerContext fireExceptionCaught(Throwable cause) { return this; }
        @Override public ChannelHandlerContext fireChannelRead(Object msg) { return this; }
        @Override public ChannelHandlerContext fireChannelReadComplete() { return this; }
        @Override public ChannelHandlerContext read() { return this; }
        @Override public ChannelFuture bind(java.net.SocketAddress localAddress, ChannelPromise promise) { promise.setSuccess(); return promise; }
        @Override public ChannelFuture connect(java.net.SocketAddress remoteAddress, java.net.SocketAddress localAddress, ChannelPromise promise) { promise.setSuccess(); return promise; }
        @Override public ChannelFuture disconnect(ChannelPromise promise) { return channel.close(promise); }
        @Override public ChannelFuture deregister(ChannelPromise promise) { promise.setSuccess(); return promise; }
    }
}
