package org.adde0109.ambassador.forge.packet;

import com.velocitypowered.api.proxy.messages.ChannelIdentifier;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ModListPacket implements IForgeLoginWrapperPacket<Context> {
    private List<String> mods;
    private Map<ChannelIdentifier, String> channels;
    private List<String> registries;
    private final List<String> dataPackRegistries;

    private final Context context;
    private ModListPacket(List<String> mods, Map<ChannelIdentifier,
            String> channels, List<String> registries, Context context, List<String> dataPackRegistries) {
        this.mods = mods;
        this.channels = channels;
        this.registries = registries;
        this.context = context;
        this.dataPackRegistries = dataPackRegistries;
    }

    public static ModListPacket read(ByteBuf input, Context context, boolean FML3) {

        List<String> mods = new ArrayList<>();
        int len = ProtocolUtils.readVarInt(input);
        for (int x = 0; x < len; x++)
            mods.add(ProtocolUtils.readString(input, 0x100));

        Map<ChannelIdentifier, String> channels = new HashMap<>();
        len = ProtocolUtils.readVarInt(input);
        for (int x = 0; x < len; x++)
            channels.put(MinecraftChannelIdentifier.from(ProtocolUtils.readString(input, 32767)),
                    ProtocolUtils.readString(input, 0x100));

        List<String> registries = new ArrayList<>();
        len = ProtocolUtils.readVarInt(input);
        for (int x = 0; x < len; x++)
            registries.add(ProtocolUtils.readString(input, 32767));

        List<String> dataPackRegistries = null;
        if (FML3) {
            dataPackRegistries = new ArrayList<>();
            len = ProtocolUtils.readVarInt(input);
            for (int x = 0; x < len; x++)
                dataPackRegistries.add(ProtocolUtils.readString(input, 0x100));
        }

        return new ModListPacket(mods, channels, registries, context, dataPackRegistries);
    }

    @Override
    public ByteBuf encode() {
        ByteBuf buf = Unpooled.buffer();

        ProtocolUtils.writeVarInt(buf, 1);

        ProtocolUtils.writeVarInt(buf, mods.size());
        mods.forEach(m -> ProtocolUtils.writeString(buf, m));

        ProtocolUtils.writeVarInt(buf, channels.size());
        channels.forEach((k, v) -> {
            ProtocolUtils.writeString(buf,k.getId());
            ProtocolUtils.writeString(buf,v);
        });

        ProtocolUtils.writeVarInt(buf, registries.size());
        registries.forEach(k -> ProtocolUtils.writeString(buf, k));

        if (dataPackRegistries != null) {
            ProtocolUtils.writeVarInt(buf, dataPackRegistries.size());
            dataPackRegistries.forEach(k -> ProtocolUtils.writeString(buf, k));
        }

        ByteBuf output = Unpooled.buffer();
        ProtocolUtils.writeString(output, "fml:handshake");
        ProtocolUtils.writeVarInt(output, buf.readableBytes());
        output.writeBytes(buf);

        return output;
    }

    @Override
    public Context getContext() {
        return context;
    }

    public List<String> getMods() {
        return mods;
    }

    public Map<ChannelIdentifier, String> getChannels() {
        return channels;
    }


}
