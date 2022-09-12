package org.adde0109.ambassador.velocity;

import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.protocol.packet.LoginPluginResponse;

public class VelocityForgeHandshakeSessionHandler implements MinecraftSessionHandler {
  @Override
  public boolean handle(LoginPluginResponse packet) {
  return true;
  }
}
