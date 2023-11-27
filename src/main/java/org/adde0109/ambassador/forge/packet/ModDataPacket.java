package org.adde0109.ambassador.forge.packet;

import io.netty.buffer.ByteBuf;

public class ModDataPacket extends GenericForgeLoginWrapperPacket<Context> {
  ModDataPacket(ByteBuf input, Context context) {
    super(input, context);
  }
}
