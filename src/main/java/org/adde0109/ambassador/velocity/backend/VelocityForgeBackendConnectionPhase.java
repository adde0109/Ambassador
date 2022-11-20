package org.adde0109.ambassador.velocity.backend;

import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.connection.backend.BackendConnectionPhase;
import com.velocitypowered.proxy.connection.backend.VelocityServerConnection;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import com.velocitypowered.proxy.protocol.packet.LoginPluginMessage;
import io.netty.util.ReferenceCountUtil;
import org.adde0109.ambassador.velocity.VelocityForgeClientConnectionPhase;

import java.util.ArrayList;
import java.util.List;

public class VelocityForgeBackendConnectionPhase implements BackendConnectionPhase {


  public VelocityForgeBackendConnectionPhase() {
  }

  public void handleSuccess(VelocityServerConnection serverCon, VelocityServer server) {
    VelocityForgeClientConnectionPhase clientPhase = ((VelocityForgeClientConnectionPhase) serverCon.getPlayer().getPhase());
    if (clientPhase.clientPhase == VelocityForgeClientConnectionPhase.ClientPhase.HANDSHAKE
            || clientPhase.clientPhase == VelocityForgeClientConnectionPhase.ClientPhase.MODLIST)
      clientPhase.complete((VelocityServer) server,serverCon.getPlayer(),serverCon.getPlayer().getConnection());
  }

  public boolean handle(VelocityServerConnection server, ConnectedPlayer player, LoginPluginMessage message) throws Exception {
    VelocityForgeClientConnectionPhase clientPhase = ((VelocityForgeClientConnectionPhase) player.getPhase());
    if (clientPhase.clientPhase == VelocityForgeClientConnectionPhase.ClientPhase.VANILLA) {
      server.getConnection().getChannel().config().setAutoRead(false);
      message.retain();
      clientPhase.reset(server,player).thenAccept((success) -> {
        if (success) {
          clientPhase.forwardPayload(server,message);
          server.getConnection().getChannel().config().setAutoRead(true);
        }
      });
    } else {
      clientPhase.forwardPayload(server, (LoginPluginMessage) message.retain());
    }
    return true;
  }


}
