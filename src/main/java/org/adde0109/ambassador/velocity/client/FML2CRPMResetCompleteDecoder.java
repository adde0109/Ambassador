package org.adde0109.ambassador.velocity.client;

import com.velocitypowered.proxy.protocol.ProtocolUtils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.adde0109.ambassador.forge.packet.Context;
import org.adde0109.ambassador.forge.packet.GenericForgeLoginWrapperPacket;
import org.adde0109.ambassador.forge.packet.IForgeLoginWrapperPacket;

public class FML2CRPMResetCompleteDecoder extends ChannelInboundHandlerAdapter {

  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) {
    if (msg instanceof ByteBuf buf) {
      if (!ctx.channel().isActive() || !buf.isReadable()) {
        buf.release();
        return;
      }

      int originalReaderIndex = buf.readerIndex();
      int packetId = ProtocolUtils.readVarInt(buf);
      if (packetId == 0x02 && buf.readableBytes() > 1) {
        try {
          int id = ProtocolUtils.readVarInt(buf);
          boolean success = buf.readBoolean();
          if (id == 98) {
            try {
              ctx.fireChannelRead(GenericForgeLoginWrapperPacket.read(
                      Unpooled.EMPTY_BUFFER, Context.createClientContext(id, success, "fml:handshake")));
            } finally {
              buf.release();
            }
            return;
          }
        } catch (Exception ignored) {}
      }
      buf.readerIndex(originalReaderIndex);
    }
    ctx.fireChannelRead(msg);
  }
}
