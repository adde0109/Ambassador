package org.adde0109.ambassador.forge.packet;

import com.velocitypowered.proxy.protocol.ProtocolUtils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public class ConfigDataPacket implements IForgeLoginWrapperPacket<Context> {

    private final String fileName;
    private final byte[] fileData;

    private final Context context;

    public ConfigDataPacket(String fileName, byte[] fileData, Context context) {
        this.fileName = fileName;
        this.fileData = fileData;
        this.context = context;
    }

    public static ConfigDataPacket read(ByteBuf input, Context context, boolean FML3) {
        String registryName = ProtocolUtils.readString(input);
        byte[] snapshot = ProtocolUtils.readByteArray(input);

        return new ConfigDataPacket(registryName, snapshot, context);
    }

    @Override
    public ByteBuf encode() {
        ByteBuf buf = Unpooled.buffer();

        ProtocolUtils.writeVarInt(buf, 4);

        ProtocolUtils.writeString(buf, fileName);
        ProtocolUtils.writeByteArray(buf, fileData);

        return buf;
    }

    @Override
    public Context getContext() {
        return context;
    }
}
