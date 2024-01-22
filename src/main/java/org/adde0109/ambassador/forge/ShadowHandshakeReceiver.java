package org.adde0109.ambassador.forge;

import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import org.adde0109.ambassador.forge.packet.*;

import java.util.HashMap;
import java.util.Map;
import java.util.zip.Adler32;
import java.util.zip.Checksum;

public class ShadowHandshakeReceiver {

  private final ConnectedPlayer player;
  private final ModListReplyPacket modListReplyPacket;
  private final Map<String, Long> registries;

  private ShadowHandshakeReceiver(ConnectedPlayer player, ModListReplyPacket modListReplyPacket,
                                 Map<String, Long> registries) {
    this.player = player;
    this.modListReplyPacket = modListReplyPacket;
    this.registries = registries;
  }

  void handle(IForgeLoginWrapperPacket packet) throws IncompatibleHandshake {

  }

  void handle(ModListPacket packet) throws IncompatibleHandshake {

    //player.getConnection().write();
  }

  void handle(RegistryPacket packet) throws IncompatibleHandshake {

    player.getConnection().write(new ACKPacket(Context.fromContext(packet.getContext(), true)));
  }
  void handle(ConfigDataPacket packet) throws IncompatibleHandshake {

    player.getConnection().write(new ACKPacket(Context.fromContext(packet.getContext(), true)));
  }

  static class Builder {

    ConnectedPlayer player;
    ModListReplyPacket modListReplyPacket;
    Map<String, Long> registries = new HashMap<>();

    void addModListPacket(ModListPacket packet) {

    }
    void setModListReplyPacket(ModListReplyPacket packet) {
      this.modListReplyPacket = packet;
    }

    void addRegistryPacket(RegistryPacket packet) {
      Checksum registryChecksum = new Adler32();
      registryChecksum.update(packet.getSnapshot());
      registries.put(packet.getRegistryName(), registryChecksum.getValue());
    }

    ShadowHandshakeReceiver build() {
      return new ShadowHandshakeReceiver(player, modListReplyPacket, registries);
    }
  }


  class IncompatibleHandshake extends Throwable {

  }
}
