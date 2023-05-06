package org.adde0109.ambassador.forge.packet;

import com.velocitypowered.api.proxy.messages.ChannelIdentifier;
import com.velocitypowered.proxy.protocol.packet.LoginPluginResponse;

import java.util.List;
import java.util.Map;

public class ModListPacket implements IForgeLoginWrapperPacket{

    private List<String> mods;
    private Map<ChannelIdentifier, String> channels;
    private List<ChannelIdentifier> registries;
    private final int id;

    public ModListPacket(int id) {
        this.id = id;
    }

    @Override
    public Object read(LoginPluginResponse message) {
        return null;
    }

    @Override
    public LoginPluginResponse encode() {
        return null;
    }

    @Override
    public int getId() {
        return 0;
    }
}
