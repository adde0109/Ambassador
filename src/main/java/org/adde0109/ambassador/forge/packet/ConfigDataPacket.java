package org.adde0109.ambassador.forge.packet;

import io.netty.buffer.ByteBuf;

public class ConfigDataPacket implements IForgeLoginWrapperPacket<Context> {

    private final Context context;

    public ConfigDataPacket(Context context) {
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
