package org.adde0109.ambassador.velocity.client;

import com.velocitypowered.proxy.protocol.netty.MinecraftEncoder;
import io.netty.channel.*;
import io.netty.handler.codec.EncoderException;


public class PluginLoginPacketQueue extends ChannelOutboundHandlerAdapter {

    private PendingWriteQueue queue;

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        queue = new PendingWriteQueue(ctx);
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        MinecraftEncoder encoder = ctx.pipeline().get(MinecraftEncoder.class);
        try {
            encoder.write(ctx, msg, promise);
        } catch (EncoderException e) {
            if (e.getCause() instanceof IllegalArgumentException) {
                queue.add(msg,promise);
            }
        }
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
        if (ctx.channel().isActive()) {
            queue.removeAndWriteAll();
            ctx.flush();
        } else {
            queue.removeAndFailAll(new ChannelException());
        }
    }
}
