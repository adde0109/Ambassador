package org.adde0109.ambassador.forge.packet;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public class GenericForgeLoginWrapperPacket<T extends Context> implements IForgeLoginWrapperPacket<T> {

  private final byte[] content;
  private final T context;

  GenericForgeLoginWrapperPacket(byte[] content, T context) {
    this.content = content;
    this.context = context;
  }


  static public GenericForgeLoginWrapperPacket<?> read(ByteBuf input, Context context) {
    byte[] content = new byte[input.readableBytes()];
    input.readBytes(content);
    return new GenericForgeLoginWrapperPacket<>(content, context);
  }

  @Override
  public ByteBuf encode() {
    ByteBuf buf = Unpooled.buffer();
    buf.writeBytes(content);
    return buf;
  }

  @Override
  public T getContext() {
    return context;
  }

}
