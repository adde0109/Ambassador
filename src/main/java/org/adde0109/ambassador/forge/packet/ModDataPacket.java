package org.adde0109.ambassador.forge.packet;

import com.velocitypowered.proxy.protocol.ProtocolUtils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public class ModDataPacket implements IForgeLoginWrapperPacket<Context> {
  private final byte[] content;
  private final Context context;

  ModDataPacket(byte[] content, Context context) {
    this.content = content;
    this.context = context;
  }


  static public ModDataPacket read(ByteBuf input, Context context) {
    byte[] content = new byte[input.readableBytes()];
    input.readBytes(content);
    return new ModDataPacket(content, context);
  }

  @Override
  public ByteBuf encode() {
    ByteBuf buf = Unpooled.buffer();
    ProtocolUtils.writeVarInt(buf, 5); //PacketID
    buf.writeBytes(content);
    return buf;
  }

  public byte[] getContent() {
    return content;
  }

  @Override
  public Context getContext() {
    return context;
  }
}
