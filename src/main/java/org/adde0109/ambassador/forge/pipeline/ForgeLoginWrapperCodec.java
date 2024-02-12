package org.adde0109.ambassador.forge.pipeline;

import com.velocitypowered.proxy.protocol.ProtocolUtils;
import com.velocitypowered.proxy.protocol.packet.LoginPluginMessagePacket;
import com.velocitypowered.proxy.protocol.packet.LoginPluginResponsePacket;
import com.velocitypowered.proxy.protocol.util.DeferredByteBufHolder;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.MessageToMessageCodec;
import org.adde0109.ambassador.Ambassador;
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

    int originalReaderIndex = buf.readerIndex();

    String channel;

    try {
      Context context;
      if (in instanceof LoginPluginMessagePacket msg && msg.getChannel().equals("fml:loginwrapper")) {
        channel = ProtocolUtils.readString(buf);
        context = Context.createContext(msg.getId(), channel);
      } else if (in instanceof LoginPluginResponsePacket msg && loginWrapperIDs.remove(Integer.valueOf(msg.getId()))) {
        channel = ProtocolUtils.readString(buf);
        context = Context.createClientContext(msg.getId(), msg.isSuccess(), channel);
      } else {
        //Not a loginWrapperPacket
        buf.readerIndex(originalReaderIndex);
        ctx.fireChannelRead(in.retain());
        return;
      }

      if (!channel.equals("fml:handshake")) {
        out.add(GenericForgeLoginWrapperPacket.read(buf, context));
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
              throw new DecoderException("Unrecognised packet ID: " + packetID);
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
              throw new DecoderException("Unrecognised packet ID: " + packetID);
          }
        }
      }
    } catch (DecoderException exception) {
      Ambassador.getInstance().logger.error("Failed to decode a wrapped Forge login packet: ", exception);
    }
  }

  @Override
  protected void encode(ChannelHandlerContext ctx, IForgeLoginWrapperPacket<?> msg, List<Object> out) throws Exception {
    ByteBuf wrapped;

    String channel = msg.getContext().getChannelName();

    wrapped = Unpooled.buffer();
    ByteBuf encoded = msg.encode();
    ProtocolUtils.writeString(wrapped, channel);
    ProtocolUtils.writeVarInt(wrapped, encoded.readableBytes());
    wrapped.writeBytes(encoded);
    encoded.release();

    if (msg.getContext() instanceof Context.ClientContext clientContext) {
      out.add(new LoginPluginResponsePacket(clientContext.getResponseID(), clientContext.success(), wrapped));
    } else {
      out.add(new LoginPluginMessagePacket(msg.getContext().getResponseID(), "fml:loginwrapper", wrapped));
      if (!(msg instanceof ModDataPacket)) {  //ModDataPacket doesn't require a response
        this.loginWrapperIDs.add(msg.getContext().getResponseID());
      }
    }
  }
}
