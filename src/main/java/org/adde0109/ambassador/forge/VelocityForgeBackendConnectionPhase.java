package org.adde0109.ambassador.forge;

import com.velocitypowered.proxy.connection.MinecraftConnection;
import com.velocitypowered.proxy.connection.backend.BackendConnectionPhase;
import com.velocitypowered.proxy.connection.backend.VelocityServerConnection;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import com.velocitypowered.proxy.protocol.packet.LoginPluginMessage;
import org.adde0109.ambassador.forge.pipeline.ForgeLoginWrapperDecoder;

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
    }

    @Override
    void onTransitionToNewPhase(VelocityServerConnection connection) {
      MinecraftConnection mc = connection.getConnection();
      if (mc != null) {
        //This looks ugly. But unless the player didn't have a FML marker, we're fine.
        mc.setType(connection.getPlayer().getConnection().getType());
      }
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

    VelocityForgeBackendConnectionPhase newPhase = getNewPhase(server,message);

    server.setConnectionPhase(newPhase);

    //Reset client if not ready to receive new handshake
    VelocityForgeClientConnectionPhase clientPhase = (VelocityForgeClientConnectionPhase) player.getPhase();
    clientPhase.resetConnectionPhase(player);
    player.getConnection().write(message.retain());

    ForgeLoginWrapperDecoder decoder = (ForgeLoginWrapperDecoder) player.getConnection()
            .getChannel().pipeline().get(ForgeConstants.FORGE_HANDSHAKE_DECODER);
    decoder.registerLoginWrapperID(message.getId());
  }

  public void onLoginSuccess(VelocityServerConnection serverCon, ConnectedPlayer player) {
  }

  void onTransitionToNewPhase(VelocityServerConnection connection) {
  }

  VelocityForgeBackendConnectionPhase nextPhase() {
    return this;
  }

  private VelocityForgeBackendConnectionPhase getNewPhase(VelocityServerConnection serverConnection,
                                                       LoginPluginMessage packet) {
    VelocityForgeBackendConnectionPhase phaseToTransitionTo = nextPhase();
    if (phaseToTransitionTo != this) {
      phaseToTransitionTo.onTransitionToNewPhase(serverConnection);
    }
    return phaseToTransitionTo;
  }

  public boolean consideredComplete() {
    return false;
  }

}
