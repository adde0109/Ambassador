package org.adde0109.ambassador.forge;

import org.adde0109.ambassador.forge.packet.ModListPacket;
import org.adde0109.ambassador.forge.packet.ModListReplyPacket;
import org.adde0109.ambassador.forge.packet.RegistryPacket;

import java.util.List;
import java.util.zip.Adler32;
import java.util.zip.Checksum;

public class ForgeHandshake {
    private ModListReplyPacket modListReplyPacket;
    private final Checksum registryChecksum = new Adler32();

    public ForgeHandshake() {
    }

    public ModListReplyPacket getModListReplyPacket() {
        return modListReplyPacket;
    }

    public void setModListReplyPacket(ModListReplyPacket modListReplyPacket) {
        this.modListReplyPacket = modListReplyPacket;
    }

    public void addRegistry(RegistryPacket packet) {
        registryChecksum.update(packet.getSnapshot());
    }

    public Checksum getRegistryChecksum() {
        return registryChecksum;
    }
}
