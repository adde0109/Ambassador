package org.adde0109.ambassador.forge.packet;

import com.velocitypowered.proxy.protocol.packet.LoginPluginResponse;
import com.velocitypowered.proxy.protocol.util.DeferredByteBufHolder;
import io.netty.buffer.ByteBuf;

public class GenericForgeLoginWrapperPacket<T extends Context> extends DeferredByteBufHolder implements IForgeLoginWrapperPacket<T> {

  private final T context;

  private GenericForgeLoginWrapperPacket(ByteBuf input, T context) {
    super(input);
    this.context = context;
  }


  static public GenericForgeLoginWrapperPacket<Context> create(ByteBuf input, Context context) {
    return new GenericForgeLoginWrapperPacket<>(input.retain(), context);
  }

  @Override
  public ByteBuf encode() {
    return content();
  }

  @Override
  public T getContext() {
    return context;
  }

}
