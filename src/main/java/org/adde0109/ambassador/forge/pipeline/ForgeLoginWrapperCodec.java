package org.adde0109.ambassador.forge.pipeline;

import com.velocitypowered.proxy.protocol.ProtocolUtils;
import com.velocitypowered.proxy.protocol.packet.LoginPluginMessage;
import com.velocitypowered.proxy.protocol.packet.LoginPluginResponse;
import com.velocitypowered.proxy.protocol.util.DeferredByteBufHolder;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.MessageToMessageCodec;
import org.adde0109.ambassador.forge.packet.*;

import java.util.ArrayList;
import java.util.List;

public class ForgeLoginWrapperCodec extends MessageToMessageCodec<DeferredByteBufHolder, IForgeLoginWrapperPacket<?>> {

  private final boolean FML3;
  private final List<Integer> loginWrapperIDs = new ArrayList<>();

  public ForgeLoginWrapperCodec(boolean fml3) {
    FML3 = fml3;
  }

  @Override
  protected void decode(ChannelHandlerContext ctx, DeferredByteBufHolder in, List<Object> out) throws Exception {
    ByteBuf buf = in.content();

    Context context;
    if (in instanceof LoginPluginMessage msg && msg.getChannel().equals("fml:loginwrapper")) {
      context = Context.createContext(msg.getId());
    } else if (in instanceof LoginPluginResponse msg && loginWrapperIDs.remove(Integer.valueOf(msg.getId()))) {
      context = Context.createContext(msg.getId(), msg.isSuccess());
    } else {
      ctx.fireChannelRead(in.retain());
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
              break;
            default:
              throw new DecoderException();
          }
        } else {
          switch (packetID) {
            case 1:
              out.add(ModListPacket.read(buf, context, FML3));
              break;
            case 3:
              out.add(RegistryPacket.read(buf, context, FML3));
              break;
            case 4:
              out.add(ConfigDataPacket.read(buf, context, FML3));
              break;
            case 5:
              if (FML3) {
                buf.readerIndex(originalReaderIndex);
                out.add(ModDataPacket.read(buf, context));
                break;
              }
            default:
              throw new DecoderException();
          }
        }
      }
    } catch (DecoderException e) {
      buf.readerIndex(originalReaderIndex);
      out.add(GenericForgeLoginWrapperPacket.read(buf, context));
    }
  }

  @Override
  protected void encode(ChannelHandlerContext ctx, IForgeLoginWrapperPacket<?> msg, List<Object> out) throws Exception {
    ByteBuf wrapped;
    if (msg instanceof GenericForgeLoginWrapperPacket<?>) {
      wrapped = msg.encode();
    } else {
      wrapped = Unpooled.buffer();
      ByteBuf encoded = msg.encode();
      ProtocolUtils.writeString(wrapped, "fml:handshake");
      ProtocolUtils.writeVarInt(wrapped, encoded.readableBytes());
      wrapped.writeBytes(encoded);
      encoded.release();
    }

    if (msg.getContext() instanceof Context.ClientContext clientContext) {
      out.add(new LoginPluginResponse(clientContext.getResponseID(), clientContext.success(), wrapped));
    } else {
      out.add(new LoginPluginMessage(msg.getContext().getResponseID(), "fml:loginwrapper", wrapped));
      if (!(msg instanceof ModDataPacket)) {
        this.loginWrapperIDs.add(msg.getContext().getResponseID());
      }
    }
  }
}
