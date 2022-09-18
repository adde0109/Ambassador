package org.adde0109.ambassador.velocity.backend;

import com.velocitypowered.proxy.connection.backend.BackendConnectionPhase;
import com.velocitypowered.proxy.connection.backend.VelocityServerConnection;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import com.velocitypowered.proxy.protocol.packet.LoginPluginMessage;
import com.velocitypowered.proxy.protocol.packet.PluginMessage;

public class VelocityForgeBackendConnectionPhase implements BackendConnectionPhase {

  public boolean handle(VelocityServerConnection server, ConnectedPlayer player, LoginPluginMessage message) {
    return true;
  }
}
