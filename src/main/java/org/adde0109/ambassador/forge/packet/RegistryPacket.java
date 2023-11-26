package org.adde0109.ambassador.forge.packet;

import io.netty.buffer.ByteBuf;

public class RegistryPacket implements IForgeLoginWrapperPacket<Context> {

  private final Context context;
  public RegistryPacket(Context context) {
    this.context = context;
  }

  @Override
  public ByteBuf encode() {
    return null;
  }

  @Override
  public Context getContext() {
    return context;
  }
}
