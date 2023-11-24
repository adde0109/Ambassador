package org.adde0109.ambassador.forge.packet;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public class ACKPacket implements IForgeLoginWrapperPacket<Context.ClientContext> {

    private final Context.ClientContext context;

    ACKPacket(int msgID, boolean success) {
        this.context = Context.createContext(msgID, success);
    }

    ACKPacket read(ByteBuf input, int msgID, boolean success) {
        return new ACKPacket(msgID, success);
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
