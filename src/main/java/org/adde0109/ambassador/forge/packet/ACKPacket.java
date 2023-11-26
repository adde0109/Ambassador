package org.adde0109.ambassador.forge.packet;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public class ACKPacket implements IForgeLoginWrapperPacket<Context.ClientContext> {

    private final Context.ClientContext context;

    public ACKPacket(Context.ClientContext context) {
        this.context = context;
    }
    @Override
    public ByteBuf encode() {
        return Unpooled.EMPTY_BUFFER;
    }

    @Override
    public Context.ClientContext getContext() {
        return context;
    }
}
