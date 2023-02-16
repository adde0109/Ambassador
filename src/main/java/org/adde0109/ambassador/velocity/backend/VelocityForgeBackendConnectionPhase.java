package org.adde0109.ambassador.velocity.backend;

import com.velocitypowered.proxy.connection.MinecraftConnection;
import com.velocitypowered.proxy.connection.backend.BackendConnectionPhase;
import com.velocitypowered.proxy.connection.backend.VelocityServerConnection;
import com.velocitypowered.proxy.connection.client.ClientConnectionPhase;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import com.velocitypowered.proxy.protocol.StateRegistry;
import com.velocitypowered.proxy.protocol.packet.LoginPluginMessage;
import io.netty.channel.PendingWriteQueue;
import org.adde0109.ambassador.forge.ForgeConstants;
import org.adde0109.ambassador.velocity.VelocityForgeClientConnectionPhase;
import org.adde0109.ambassador.velocity.client.OutboundSuccessHolder;

public enum VelocityForgeBackendConnectionPhase implements BackendConnectionPhase {
  NOT_STARTED() {
    @Override
    VelocityForgeBackendConnectionPhase nextPhase() {
      return WAITING_FOR_ACK;
    }
  },
  WAITING_FOR_ACK() {
    @Override
    public void onLoginSuccess(VelocityServerConnection serverCon, ConnectedPlayer player) {
      serverCon.setConnectionPhase(VelocityForgeBackendConnectionPhase.COMPLETE);

      MinecraftConnection connection = player.getConnection();
      ((OutboundSuccessHolder) connection.getChannel().pipeline().get(ForgeConstants.SERVER_SUCCESS_LISTENER))
              .sendPacket();
      connection.setState(StateRegistry.PLAY);
    }
  },

  COMPLETE() {
    @Override
    public boolean consideredComplete() {
      return true;
    }
  };



  VelocityForgeBackendConnectionPhase() {
  }

  public void handle(VelocityServerConnection server, ConnectedPlayer player, LoginPluginMessage message) {

    VelocityForgeBackendConnectionPhase newPhase = nextPhase();

    server.setConnectionPhase(newPhase);

    //Reset client if not ready to receive new handshake
    VelocityForgeClientConnectionPhase clientPhase = (VelocityForgeClientConnectionPhase) player.getPhase();
    clientPhase.resetConnectionPhase(player);
    player.getConnection().write(message.retain());
  }

  public void onLoginSuccess(VelocityServerConnection serverCon, ConnectedPlayer player) {

  }
  VelocityForgeBackendConnectionPhase nextPhase() {
    return this;
  }
  public boolean consideredComplete() {
    return false;
  }

}
