package io.netty.channel;

import java.lang.annotation.*;

public interface ChannelHandler {
    default void handlerAdded(ChannelHandlerContext ctx) throws Exception {}
    default void handlerRemoved(ChannelHandlerContext ctx) throws Exception {}
    default void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        ctx.fireExceptionCaught(cause);
    }

    @Inherited
    @Documented
    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    @interface Sharable {}
}
