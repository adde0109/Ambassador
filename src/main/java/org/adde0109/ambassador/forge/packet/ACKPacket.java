package org.adde0109.ambassador.forge.packet;

import com.velocitypowered.proxy.protocol.ProtocolUtils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public class ACKPacket implements IForgeLoginWrapperPacket<Context.ClientContext> {

    private final Context.ClientContext context;

    public ACKPacket(Context.ClientContext context) {
        this.context = context;
    }
    @Override
    public ByteBuf encode() {
        ByteBuf buf = Unpooled.buffer();

        ProtocolUtils.writeVarInt(buf, 99);

        return buf;
    }

    @Override
    public Context.ClientContext getContext() {
        return context;
    }
}
