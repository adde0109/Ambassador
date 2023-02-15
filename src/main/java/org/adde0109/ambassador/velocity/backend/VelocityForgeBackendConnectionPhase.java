package org.adde0109.ambassador.velocity.backend;

import com.velocitypowered.proxy.connection.MinecraftConnection;
import com.velocitypowered.proxy.connection.backend.BackendConnectionPhase;
import com.velocitypowered.proxy.connection.backend.VelocityServerConnection;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import com.velocitypowered.proxy.protocol.StateRegistry;
import com.velocitypowered.proxy.protocol.packet.LoginPluginMessage;
import org.adde0109.ambassador.forge.ForgeConstants;
import org.adde0109.ambassador.velocity.VelocityForgeClientConnectionPhase;

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
      connection.getChannel().pipeline().remove(ForgeConstants.SERVER_SUCCESS_LISTENER);
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

    VelocityForgeClientConnectionPhase clientPhase = ((VelocityForgeClientConnectionPhase) player.getPhase());
    if (player.getConnection().getState() != StateRegistry.LOGIN) {
      final LoginPluginMessage msg = message;

      msg.content().retain().discardSomeReadBytes();

      server.getConnection().getChannel().config().setAutoRead(false);
      clientPhase.reset(server.getServer(),player).thenAccept((success) -> {
        if (success) {
          player.getConnection().write(msg);
          server.getConnection().getChannel().config().setAutoRead(true);
        } else {
          msg.release();
        }
      });
    } else {
      player.getConnection().write(message.retain());
    }
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
