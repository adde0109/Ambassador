package org.adde0109.ambassador.forge.pipeline;

import com.velocitypowered.proxy.protocol.ProtocolUtils;
import com.velocitypowered.proxy.protocol.packet.LoginPluginResponse;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import org.adde0109.ambassador.forge.packet.GenericForgeLoginWrapperPacket;
import org.adde0109.ambassador.forge.packet.ModListReplyPacket;

import java.util.ArrayList;
import java.util.List;

public class ForgeLoginWrapperDecoder extends MessageToMessageDecoder<LoginPluginResponse> {

  private final List<Integer> loginWrapperIDs = new ArrayList<>();

  @Override
  protected void decode(ChannelHandlerContext ctx, LoginPluginResponse msg, List<Object> out) throws Exception {
    ByteBuf buf = msg.content();
    if (!loginWrapperIDs.remove((Integer) msg.getId())) {
      out.add(msg.retain());
      return;
    }
    int originalReaderIndex = msg.content().readerIndex();
    String channel = ProtocolUtils.readString(buf);
    if (!channel.equals("fml:handshake")) {
      buf.readerIndex(originalReaderIndex);
      out.add(new GenericForgeLoginWrapperPacket(buf.retain(), msg.getId(), true));
      return;
    }
    int length = ProtocolUtils.readVarInt(buf);
    int packetID = ProtocolUtils.readVarInt(buf);
    if (packetID == 2) {
      out.add(ModListReplyPacket.read(msg));
    } else {
      buf.readerIndex(originalReaderIndex);
      out.add(new GenericForgeLoginWrapperPacket(buf.retain(), msg.getId(), true));
    }
  }

  public void registerLoginWrapperID(int loginWrapperID) {
    this.loginWrapperIDs.add(loginWrapperID);
  }
}
