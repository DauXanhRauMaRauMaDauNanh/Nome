package io.netty.channel;

import java.util.concurrent.ScheduledExecutorService;

public interface EventLoop extends ScheduledExecutorService {
    ChannelFuture register(Channel channel);
    ChannelFuture register(ChannelPromise promise);
    boolean inEventLoop();
    boolean inEventLoop(Thread thread);
}
