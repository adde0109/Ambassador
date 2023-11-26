package org.adde0109.ambassador.forge.pipeline;

import com.velocitypowered.proxy.protocol.ProtocolUtils;
import com.velocitypowered.proxy.protocol.packet.LoginPluginMessage;
import com.velocitypowered.proxy.protocol.packet.LoginPluginResponse;
import com.velocitypowered.proxy.protocol.util.DeferredByteBufHolder;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.MessageToMessageCodec;
import org.adde0109.ambassador.forge.packet.*;

import java.util.ArrayList;
import java.util.List;

public class ForgeLoginWrapperCodec extends MessageToMessageCodec<DeferredByteBufHolder, IForgeLoginWrapperPacket<?>> {

  private final List<Integer> loginWrapperIDs = new ArrayList<>();

  @Override
  protected void decode(ChannelHandlerContext ctx, DeferredByteBufHolder in, List<Object> out) throws Exception {
    ByteBuf buf = in.content();

    Context context;
    if (in instanceof LoginPluginMessage msg && msg.getChannel().equals("fml:loginwrapper")) {
      context = Context.createContext(msg.getId());
    } else if (in instanceof LoginPluginResponse msg && loginWrapperIDs.remove(msg.getId()) != null) {
      context = Context.createContext(msg.getId(), msg.isSuccess());
    } else {
      return;
    }

    int originalReaderIndex = buf.readerIndex();
    try {
      String channel = ProtocolUtils.readString(buf);
      if (!channel.equals("fml:handshake")) {
        throw new DecoderException();
      } else {
        int length = ProtocolUtils.readVarInt(buf);
        int packetID = ProtocolUtils.readVarInt(buf);
        if (context instanceof Context.ClientContext clientContext) {
          switch (packetID) {
            case 2:
              out.add(ModListReplyPacket.read(buf, clientContext));
              break;
            case 99:
              out.add(new ACKPacket(clientContext));
            default:
              throw new DecoderException();
          }
        } else {
          switch (packetID) {
            case 1:
              out.add(ModListPacket.read(buf, context));
              break;
            case 3:
              out.add(new RegistryPacket(context));
              break;
            case 4:
              out.add(new ConfigDataPacket(context));
              break;
            default:
              throw new DecoderException();
          }
        }
      }
    } catch (DecoderException e) {
      buf.readerIndex(originalReaderIndex);
      out.add(GenericForgeLoginWrapperPacket.create(buf.retain(), context));
    }
  }

  @Override
  protected void encode(ChannelHandlerContext ctx, IForgeLoginWrapperPacket<?> msg, List<Object> out) throws Exception {
    if (msg.getContext() instanceof Context.ClientContext clientContext) {
      out.add(new LoginPluginResponse(clientContext.getResponseID(), clientContext.success(), msg.encode()));
    } else {
      out.add(new LoginPluginMessage(msg.getContext().getResponseID(), "fml:loginwrapper", msg.encode()));
      this.loginWrapperIDs.add(msg.getContext().getResponseID());
    }
  }
}
