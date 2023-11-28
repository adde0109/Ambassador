package org.adde0109.ambassador.forge.packet;

import com.velocitypowered.proxy.protocol.ProtocolUtils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public class RegistryPacket implements IForgeLoginWrapperPacket<Context> {

  private final String registryName;

  private final byte[] snapshot;

  private final Context context;
  public RegistryPacket(String registryName, byte[] snapshot, Context context) {
    this.registryName = registryName;
    this.snapshot = snapshot;
    this.context = context;
  }

  public static RegistryPacket read(ByteBuf input, Context context, boolean FML3) {
    String registryName = ProtocolUtils.readString(input);
    byte[] snapshot = null;
    if (input.readBoolean()) {
      snapshot = new byte[input.readableBytes()];
      input.readBytes(snapshot);
    }

    return new RegistryPacket(registryName, snapshot, context);
  }

  @Override
  public ByteBuf encode() {
    ByteBuf buf = Unpooled.buffer();

    ProtocolUtils.writeVarInt(buf, 3);

    ProtocolUtils.writeString(buf, registryName);
    if (snapshot != null) {
      buf.writeBoolean(true);
      buf.writeBytes(snapshot);
    } else {
      buf.writeBoolean(false);
    }

    return buf;
  }

  @Override
  public Context getContext() {
    return context;
  }

  public String getRegistryName() {
    return registryName;
  }

  public byte[] getSnapshot() {
    return snapshot;
  }
}
