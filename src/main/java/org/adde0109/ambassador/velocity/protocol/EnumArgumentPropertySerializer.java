package org.adde0109.ambassador.velocity.protocol;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import com.velocitypowered.proxy.protocol.packet.brigadier.ArgumentPropertySerializer;
import io.netty.buffer.ByteBuf;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * An argument property serializer that will serialize and deserialize nothing.
 */
public class EnumArgumentPropertySerializer implements ArgumentPropertySerializer<EnumArgumentProperty> {

  public static final EnumArgumentPropertySerializer ENUM = new EnumArgumentPropertySerializer();

  private EnumArgumentPropertySerializer() {
  }

  @Override
  public @Nullable EnumArgumentProperty deserialize(ByteBuf buf, ProtocolVersion protocolVersion) {
    return new EnumArgumentProperty(ProtocolUtils.readString(buf));
  }

  @Override
  public void serialize(EnumArgumentProperty object, ByteBuf buf, ProtocolVersion protocolVersion) {
    ProtocolUtils.writeString(buf, object.getClassName());
  }
}
