package org.adde0109.ambassador.forge;

import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import com.velocitypowered.proxy.protocol.packet.LoginPluginResponse;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

public class FML2CRPMResetCompleteDecoder extends ChannelInboundHandlerAdapter {

  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
    if (msg instanceof ByteBuf buf) {
      if (!ctx.channel().isActive() || !buf.isReadable()) {
        buf.release();
        return;
      }

      int originalReaderIndex = buf.readerIndex();
      int packetId = ProtocolUtils.readVarInt(buf);
      if (packetId == 0x02 && buf.readableBytes() > 1) {
        try {
          MinecraftPacket packet = new LoginPluginResponse();
          packet.decode(buf, ProtocolUtils.Direction.SERVERBOUND,null);
          ctx.fireChannelRead(packet);
        } finally {
          buf.release();
        }
        return;
      } else {
        buf.readerIndex(originalReaderIndex);
      }
    }
    ctx.fireChannelRead(msg);
  }
}
