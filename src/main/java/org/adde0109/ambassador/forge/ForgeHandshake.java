package org.adde0109.ambassador.forge;

import org.adde0109.ambassador.forge.packet.ModListPacket;
import org.adde0109.ambassador.forge.packet.ModListReplyPacket;

public class ForgeHandshake {

    private ModListPacket modListPacket;
    private ModListReplyPacket modListReplyPacket;

    public ForgeHandshake() {

    }

    public ModListPacket getModListPacket() {
        return modListPacket;
    }

    public void setModListPacket(ModListPacket modListPacket) {
        this.modListPacket = modListPacket;
    }

    public ModListReplyPacket getModListReplyPacket() {
        return modListReplyPacket;
    }

    public void setModListReplyPacket(ModListReplyPacket modListReplyPacket) {
        this.modListReplyPacket = modListReplyPacket;
    }
}
