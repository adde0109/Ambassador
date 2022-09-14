package org.adde0109.ambassador.velocity;

import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.connection.client.ClientConnectionPhase;
import com.velocitypowered.proxy.protocol.packet.LoginPluginResponse;

public class VelocityForgeHandshakeSessionHandler implements MinecraftSessionHandler {

  private final VelocityForgeClientConnectionPhase phase;
  public VelocityForgeHandshakeSessionHandler(VelocityForgeClientConnectionPhase phase) {
    this.phase = phase;
  }

  @Override
  public boolean handle(LoginPluginResponse packet) {
    //TODO: Check if we sent it
    if (packet.getId() == 1 && packet.getId() == 98) {
      phase.handle(packet);
    }
    return true;
  }
}
