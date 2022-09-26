package org.adde0109.ambassador.velocity.backend;

import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.connection.backend.BackendConnectionPhase;
import com.velocitypowered.proxy.connection.backend.VelocityServerConnection;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import com.velocitypowered.proxy.protocol.StateRegistry;
import com.velocitypowered.proxy.protocol.packet.LoginPluginMessage;
import org.adde0109.ambassador.forge.ForgeFML2ClientConnectionPhase;

import java.util.ArrayList;
import java.util.List;

public class VelocityForgeBackendConnectionPhase implements BackendConnectionPhase {

  private final List<LoginPluginMessage> queuedHandshakePackets = new ArrayList<>();

  public VelocityForgeBackendConnectionPhase() {
  }

  public void handleSuccess(VelocityServerConnection serverCon, VelocityServer server) {
    ForgeFML2ClientConnectionPhase clientPhase = ((ForgeFML2ClientConnectionPhase) serverCon.getPlayer().getPhase());
    if (clientPhase.clientPhase == ForgeFML2ClientConnectionPhase.ClientPhase.HANDSHAKE || clientPhase.clientPhase == ForgeFML2ClientConnectionPhase.ClientPhase.MODLIST) {
      clientPhase.complete((VelocityServer) server,serverCon.getPlayer(),serverCon.getPlayer().getConnection());
    }
  }

  public boolean handle(VelocityServerConnection server, ConnectedPlayer player, LoginPluginMessage message) throws Exception {
    ForgeFML2ClientConnectionPhase clientPhase = ((ForgeFML2ClientConnectionPhase) player.getPhase());
    message.retain();
    if (clientPhase.clientPhase == ForgeFML2ClientConnectionPhase.ClientPhase.VANILLA) {
      clientPhase.reset(player, () -> {
        for (LoginPluginMessage msg: queuedHandshakePackets) {
          player.getConnection().delayedWrite(msg);
        }
        player.getConnection().flush();
      });
      queuedHandshakePackets.add(message);
    } else if (clientPhase.clientPhase != null) {
      player.getConnection().write(message);
    } else {
      queuedHandshakePackets.add(message);
    }
    return true;
  }


}
