package org.adde0109.ambassador.forge.packet;

import com.velocitypowered.proxy.protocol.packet.LoginPluginResponse;
import com.velocitypowered.proxy.protocol.util.DeferredByteBufHolder;
import io.netty.buffer.ByteBuf;

public class GenericForgeLoginWrapperPacket extends DeferredByteBufHolder implements IForgeLoginWrapperPacket {
  private final int id;

  public GenericForgeLoginWrapperPacket(ByteBuf input, int id) {
    super(input);
    this.id = id;
  }

  @Override
  public LoginPluginResponse encode() {
    return new LoginPluginResponse(id, true, content());
  }

  @Override
  public int getId() {
    return id;
  }
}
