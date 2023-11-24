package org.adde0109.ambassador.forge.packet;

import io.netty.buffer.ByteBuf;

public class configDataPacket implements IForgeLoginWrapperPacket<Context> {

    private final Context context;

    public configDataPacket(int msgID) {
        this.context = Context.createContext(msgID);
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
