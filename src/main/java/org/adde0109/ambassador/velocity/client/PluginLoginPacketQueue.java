package org.adde0109.ambassador.velocity.client;

import com.velocitypowered.proxy.connection.MinecraftConnection;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import com.velocitypowered.proxy.protocol.StateRegistry;
import com.velocitypowered.proxy.protocol.netty.MinecraftEncoder;
import com.velocitypowered.proxy.protocol.util.DeferredByteBufHolder;
import io.netty.channel.*;
import io.netty.handler.codec.EncoderException;
import io.netty.util.ReferenceCountUtil;


public class PluginLoginPacketQueue extends ChannelOutboundHandlerAdapter {

    private PendingWriteQueue queue;

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        queue = new PendingWriteQueue(ctx);
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        MinecraftConnection connection = ctx.pipeline().get(MinecraftConnection.class);
        if (connection.getState() == StateRegistry.LOGIN && msg instanceof MinecraftPacket packet) {
            try {
                StateRegistry.LOGIN.getProtocolRegistry(ProtocolUtils.Direction.CLIENTBOUND ,
                        connection.getProtocolVersion()).getPacketId(packet);
                ctx.write(msg,promise);
            } catch (IllegalArgumentException e) {
                queue.add(msg, promise);
            }
        } else {
            ctx.write(msg,promise);
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
