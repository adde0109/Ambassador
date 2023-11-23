package org.adde0109.ambassador.forge.packet;

import io.netty.buffer.ByteBuf;

public interface IForgeLoginWrapperPacket<T extends Context> {
  ByteBuf encode();
  T getContext();
}
