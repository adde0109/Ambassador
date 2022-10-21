package org.adde0109.ambassador.velocity.backend;

import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.connection.backend.BackendConnectionPhase;
import com.velocitypowered.proxy.connection.backend.VelocityServerConnection;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import com.velocitypowered.proxy.protocol.packet.LoginPluginMessage;
import io.netty.util.ReferenceCountUtil;
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
    clientPhase.complete((VelocityServer) server,serverCon.getPlayer(),serverCon.getPlayer().getConnection());
  }

  public boolean handle(VelocityServerConnection server, ConnectedPlayer player, LoginPluginMessage message) throws Exception {
    VelocityForgeClientConnectionPhase clientPhase = ((VelocityForgeClientConnectionPhase) player.getPhase());
    message.retain();
    if (clientPhase.clientPhase == VelocityForgeClientConnectionPhase.ClientPhase.VANILLA) {
      clientPhase.reset(server,player).thenAccept((success) -> {
        if (success) {
          for (LoginPluginMessage msg: queuedHandshakePackets) {
            ((VelocityForgeClientConnectionPhase) player.getPhase()).forwardPayload(server,msg);
          }
          player.getConnection().flush();
        } else {
          for (LoginPluginMessage msg: queuedHandshakePackets) {
            ReferenceCountUtil.release(msg);
          }
        }
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
