package org.adde0109.ambassador.velocity.client;

import com.velocitypowered.proxy.connection.MinecraftConnection;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import com.velocitypowered.proxy.protocol.StateRegistry;
import io.netty.channel.*;


public class ClientPacketQueue extends ChannelOutboundHandlerAdapter {

    private PendingWriteQueue queue;
    private final StateRegistry allow;

    public ClientPacketQueue(StateRegistry registry) {
        this.allow = registry;
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        queue = new PendingWriteQueue(ctx);
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        MinecraftConnection connection = ctx.pipeline().get(MinecraftConnection.class);
        if (msg instanceof MinecraftPacket packet) {
            try {
                allow.getProtocolRegistry(ProtocolUtils.Direction.CLIENTBOUND ,
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
