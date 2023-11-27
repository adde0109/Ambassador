package org.adde0109.ambassador.forge.packet;

import io.netty.buffer.ByteBuf;

public class ModDataPacket extends GenericForgeLoginWrapperPacket<Context> {

  ModDataPacket(byte[] content, Context context) {
    super(content, context);
  }
}
