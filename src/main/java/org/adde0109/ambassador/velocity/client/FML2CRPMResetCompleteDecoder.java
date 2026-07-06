package org.adde0109.ambassador.velocity.client;

import com.velocitypowered.proxy.connection.MinecraftConnection;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import com.velocitypowered.proxy.protocol.StateRegistry;
import com.velocitypowered.proxy.protocol.packet.LoginPluginResponsePacket;
import com.velocitypowered.proxy.protocol.packet.PluginMessagePacket;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
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

      if (tryHandleResetComplete(ctx, buf)) {
        buf.release();
        return;
      }
      buf.release();
      return;
    } else if (msg instanceof LoginPluginResponsePacket packet && packet.getId() == 98) {
      ctx.fireChannelRead(new GenericForgeLoginWrapperPacket(Unpooled.EMPTY_BUFFER, 98, packet.isSuccess()));
      packet.release();
      return;
    } else if (msg instanceof LoginPluginResponsePacket packet) {
      packet.release();
      return;
    }
    ctx.fireChannelRead(msg);
  }

  private boolean tryHandleResetComplete(ChannelHandlerContext ctx, ByteBuf buf) {
    int originalReaderIndex = buf.readerIndex();
    try {
      MinecraftConnection connection = ctx.pipeline().get(MinecraftConnection.class);
      if (connection == null) {
        return false;
      }

      int packetId = ProtocolUtils.readVarInt(buf);
      if (isLoginPluginResponse(connection, packetId)) {
        return tryHandleLoginPluginResponse(ctx, buf);
      }

      if (isPluginMessage(connection, packetId)) {
        return tryHandlePluginMessage(ctx, buf);
      }
      return false;
    } catch (Exception ignored) {
      return false;
    } finally {
      buf.readerIndex(originalReaderIndex);
    }
  }

  private boolean isLoginPluginResponse(MinecraftConnection connection, int packetId) {
    return getPacketId(connection, StateRegistry.LOGIN, new LoginPluginResponsePacket()) == packetId;
  }

  private boolean isPluginMessage(MinecraftConnection connection, int packetId) {
    return getPacketId(connection, StateRegistry.PLAY, new PluginMessagePacket()) == packetId
            || getPacketId(connection, StateRegistry.CONFIG, new PluginMessagePacket()) == packetId;
  }

  private int getPacketId(MinecraftConnection connection, StateRegistry state, MinecraftPacket packet) {
    try {
      return state.getProtocolRegistry(ProtocolUtils.Direction.SERVERBOUND,
              connection.getProtocolVersion()).getPacketId(packet);
    } catch (IllegalArgumentException ignored) {
      return -1;
    }
  }

  private boolean tryHandleLoginPluginResponse(ChannelHandlerContext ctx, ByteBuf buf) {
    if (buf.readableBytes() < 2) {
      return false;
    }
    int id = ProtocolUtils.readVarInt(buf);
    boolean success = buf.readBoolean();
    if (id != 98) {
      return false;
    }
    IForgeLoginWrapperPacket packet = new GenericForgeLoginWrapperPacket(Unpooled.EMPTY_BUFFER, id, success);
    ctx.fireChannelRead(packet);
    return true;
  }

  private boolean tryHandlePluginMessage(ChannelHandlerContext ctx, ByteBuf buf) {
    String channel = ProtocolUtils.readString(buf);
    if (!channel.equals("fml:handshake")) {
      return false;
    }
    int fmlPacketId = ProtocolUtils.readVarInt(buf);
    if (fmlPacketId != 98 && fmlPacketId != 99) {
      return false;
    }
    IForgeLoginWrapperPacket packet = new GenericForgeLoginWrapperPacket(Unpooled.EMPTY_BUFFER, 98, true);
    ctx.fireChannelRead(packet);
    return true;
  }
}
