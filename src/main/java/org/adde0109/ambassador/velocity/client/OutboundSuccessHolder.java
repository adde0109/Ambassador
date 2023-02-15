package org.adde0109.ambassador.velocity.client;

import com.velocitypowered.proxy.protocol.packet.ServerLoginSuccess;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;

public class OutboundSuccessHolder extends ChannelOutboundHandlerAdapter {

  private ServerLoginSuccess packet;

  @Override
  public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
    if ((msg instanceof ServerLoginSuccess packet)) {
      this.packet = packet;
    } else {
      ctx.write(msg, promise);
    }
  }

  @Override
  public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
    if (ctx.channel().isActive()) {
      ctx.write(packet, ctx.voidPromise());
      ctx.flush();
    }
  }
}
