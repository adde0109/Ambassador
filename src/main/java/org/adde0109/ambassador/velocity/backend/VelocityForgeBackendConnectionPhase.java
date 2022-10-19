package org.adde0109.ambassador.velocity.backend;

import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.connection.backend.BackendConnectionPhase;
import com.velocitypowered.proxy.connection.backend.VelocityServerConnection;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import com.velocitypowered.proxy.protocol.packet.LoginPluginMessage;
import org.adde0109.ambassador.forge.FML2CRPMClientConnectionPhase;
import org.adde0109.ambassador.velocity.VelocityForgeClientConnectionPhase;

import java.util.ArrayList;
import java.util.List;

public class VelocityForgeBackendConnectionPhase implements BackendConnectionPhase {

  private List<LoginPluginMessage> queuedHandshakePackets = new ArrayList<>();

  public VelocityForgeBackendConnectionPhase() {
  }

  public void handleSuccess(VelocityServerConnection serverCon, VelocityServer server) {
    VelocityForgeClientConnectionPhase clientPhase = ((VelocityForgeClientConnectionPhase) serverCon.getPlayer().getPhase());
    if (clientPhase.clientPhase == VelocityForgeClientConnectionPhase.ClientPhase.HANDSHAKE || clientPhase.clientPhase == VelocityForgeClientConnectionPhase.ClientPhase.MODLIST) {
      clientPhase.complete((VelocityServer) server,serverCon.getPlayer(),serverCon.getPlayer().getConnection());
    }
  }

  public boolean handle(VelocityServerConnection server, ConnectedPlayer player, LoginPluginMessage message) throws Exception {
    VelocityForgeClientConnectionPhase clientPhase = ((FML2CRPMClientConnectionPhase) player.getPhase());
    message.retain();
    if (clientPhase.clientPhase == FML2CRPMClientConnectionPhase.ClientPhase.VANILLA) {
      clientPhase.reset(server,player, () -> {
        for (LoginPluginMessage msg: queuedHandshakePackets) {
          clientPhase.forwardPayload(server,msg);
        }
        player.getConnection().flush();
        queuedHandshakePackets = null;
      });
      queuedHandshakePackets = new ArrayList<>();
      queuedHandshakePackets.add(message);
    } else if (clientPhase.clientPhase != null) {
      clientPhase.forwardPayload(server,message);
    } else {
      queuedHandshakePackets.add(message);
    }
    return true;
  }


}