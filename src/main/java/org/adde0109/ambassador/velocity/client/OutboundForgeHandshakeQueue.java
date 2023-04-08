package org.adde0109.ambassador.velocity.client;

import com.velocitypowered.proxy.protocol.packet.LoginPluginMessage;
import io.netty.channel.*;

public class OutboundForgeHandshakeQueue extends ChannelOutboundHandlerAdapter {

  PendingWriteQueue writeQueue;

  @Override
  public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
    writeQueue = new PendingWriteQueue(ctx);
  }

  @Override
  public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
    if ((msg instanceof LoginPluginMessage packet)) {
      writeQueue.add(msg, promise);
    } else {
      ctx.write(msg, promise);
    }
  }

  @Override
  public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
    if (ctx.channel().isActive()) {
      writeQueue.removeAndWriteAll();
      ctx.flush();
    } else {
      writeQueue.removeAndFailAll(new ChannelException());
    }
  }
}
