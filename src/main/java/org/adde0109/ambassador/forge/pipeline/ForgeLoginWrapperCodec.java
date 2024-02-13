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
import io.netty.util.ReferenceCountUtil;
import org.adde0109.ambassador.Ambassador;
import org.adde0109.ambassador.forge.packet.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ForgeLoginWrapperCodec extends MessageToMessageCodec<DeferredByteBufHolder, IForgeLoginWrapperPacket<?>> {

  private final boolean FML3;
  private final Map<Integer, Context> loginWrapperContexts = new HashMap<>();

  public ForgeLoginWrapperCodec(boolean fml3) {
    FML3 = fml3;
  }

  @Override
  public boolean acceptInboundMessage(Object msg) throws Exception {
    return (msg instanceof LoginPluginMessagePacket
            && ((LoginPluginMessagePacket) msg).getChannel().equals("fml:loginwrapper"))
            || (msg instanceof LoginPluginResponsePacket
            && loginWrapperContexts.containsKey(((LoginPluginResponsePacket) msg).getId()));
  }

  @Override
  protected void decode(ChannelHandlerContext ctx, DeferredByteBufHolder in, List<Object> out) throws Exception {

    ByteBuf buf = in.content();

    Context context;
    if (in instanceof LoginPluginResponsePacket msg) {
      //Continue from stored context
      context = Context.fromContext(
              loginWrapperContexts.remove(((LoginPluginResponsePacket) msg).getId()), msg.isSuccess());
      if (!msg.isSuccess()) {
        //Nothing to read, just create an empty packet.
        out.add(GenericForgeLoginWrapperPacket.read(buf, context));
        return;
      } else {
        String channel = ProtocolUtils.readString(buf); //Read the channel even though we know the channel by context.
        Ambassador.getInstance();
      }
    } else {
      //New context.
      LoginPluginMessagePacket msg = (LoginPluginMessagePacket) in;
      String channel = ProtocolUtils.readString(buf);

      context = Context.createContext(msg.getId(), channel);
    }

    //Decoding of data starts here - channel already read
    int originalReaderIndex = buf.readerIndex();

    if (!context.getChannelName().equals("fml:handshake")) {
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
            //Undo decoding
            buf.readerIndex(originalReaderIndex);
            out.add(GenericForgeLoginWrapperPacket.read(buf, context));
            if (Ambassador.getInstance().config.isDebugMode()) {
              Ambassador.getInstance().logger.warn(
                      "Unrecognised packet id received from client on fml:handshake: " + packetID);
            }
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
              out.add(ModDataPacket.read(buf, context));
              break;
            }
          default:
            //Undo decoding
            buf.readerIndex(originalReaderIndex);
            out.add(GenericForgeLoginWrapperPacket.read(buf, context));
            if (Ambassador.getInstance().config.isDebugMode()) {
              Ambassador.getInstance().logger.warn(
                      "Unrecognised packet id received from server on fml:handshake: " + packetID);
            }
        }
      }
    }
  }

  @Override
  protected void encode(ChannelHandlerContext ctx, IForgeLoginWrapperPacket<?> msg, List<Object> out) throws Exception {
    ByteBuf wrapped;

    boolean data = !(msg.getContext() instanceof Context.ClientContext clientContext && !clientContext.success());

    boolean includeLength = !(msg instanceof GenericForgeLoginWrapperPacket);

    String channel = msg.getContext().getChannelName();

    wrapped = Unpooled.buffer();

    if (data) {
      ByteBuf encoded = msg.encode();
      ProtocolUtils.writeString(wrapped, channel);
      if (includeLength)
        ProtocolUtils.writeVarInt(wrapped, encoded.readableBytes());
      wrapped.writeBytes(encoded);
      encoded.release();
    }
    if (msg.getContext() instanceof Context.ClientContext clientContext) {
      out.add(new LoginPluginResponsePacket(clientContext.getResponseID(), clientContext.success(), wrapped));
    } else {
      out.add(new LoginPluginMessagePacket(msg.getContext().getResponseID(), "fml:loginwrapper", wrapped));
      if (!(msg instanceof ModDataPacket)) {  //ModDataPacket doesn't require a response
        this.loginWrapperContexts.put(msg.getContext().getResponseID(), msg.getContext());
      }
    }
  }
}
