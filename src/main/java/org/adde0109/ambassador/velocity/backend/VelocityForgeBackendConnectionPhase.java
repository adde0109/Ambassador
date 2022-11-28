package org.adde0109.ambassador.velocity.backend;

import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.connection.backend.BackendConnectionPhase;
import com.velocitypowered.proxy.connection.backend.VelocityServerConnection;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import com.velocitypowered.proxy.protocol.packet.LoginPluginMessage;
import io.netty.buffer.ByteBuf;
import io.netty.util.ReferenceCountUtil;
import org.adde0109.ambassador.velocity.VelocityForgeClientConnectionPhase;

import java.util.ArrayList;
import java.util.List;

public class VelocityForgeBackendConnectionPhase implements BackendConnectionPhase {


  public VelocityForgeBackendConnectionPhase() {
  }

  public void handle(VelocityServerConnection server, ConnectedPlayer player, LoginPluginMessage message) {
    VelocityForgeClientConnectionPhase clientPhase = ((VelocityForgeClientConnectionPhase) player.getPhase());
    if (clientPhase.clientPhase == VelocityForgeClientConnectionPhase.ClientPhase.VANILLA) {
      final LoginPluginMessage msg = message;

      msg.content().retain().discardSomeReadBytes();

      server.getConnection().getChannel().config().setAutoRead(false);
      clientPhase.reset(server.getServer(),player).thenAccept((success) -> {
        if (success) {
          clientPhase.forwardPayload(server,msg);
          server.getConnection().getChannel().config().setAutoRead(true);
        } else {
          msg.release();
        }
      });
    } else {
      clientPhase.forwardPayload(server, (LoginPluginMessage) message.retain());
    }
  }
}
