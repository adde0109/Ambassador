package org.adde0109.ambassador.velocity;

import com.velocitypowered.api.event.Continuation;
import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.connection.MinecraftConnection;
import com.velocitypowered.proxy.connection.backend.VelocityServerConnection;
import com.velocitypowered.proxy.connection.client.ClientConnectionPhase;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import com.velocitypowered.proxy.protocol.packet.LoginPluginMessage;
import com.velocitypowered.proxy.protocol.packet.LoginPluginResponse;
import org.adde0109.ambassador.forge.FML2CRPMClientConnectionPhase;
import org.adde0109.ambassador.forge.ForgeHandshakeUtils;

import javax.annotation.Nullable;
import java.util.ArrayList;

public abstract class VelocityForgeClientConnectionPhase implements ClientConnectionPhase {
  //TODO:Make class when PCF is done


  VelocityLoginPayloadManager payloadManager;
  public FML2CRPMClientConnectionPhase.ClientPhase clientPhase = ClientPhase.HANDSHAKE;

  public void handleLogin(ConnectedPlayer player, VelocityServer server, Continuation continuation) {
  }

  public void reset(VelocityServerConnection serverConnection,ConnectedPlayer player, Runnable whenComplete) {
  }

  public void complete(VelocityServer server, ConnectedPlayer player, MinecraftConnection connection) {

  }

  final void fireLoginEvent(ConnectedPlayer player, VelocityServer server, Continuation continuation) {
    payloadManager = new VelocityLoginPayloadManager(player.getConnection());
    handleLogin(player,server,continuation);
  }

  public void forwardPayload(VelocityServerConnection serverConnection, LoginPluginMessage payload) {
    if (payloadManager == null) {
      return;
    }
    payloadManager.sendPayload("fml:loginwrapper",payload.content()).thenAccept((responseData) -> {
      //Move this to the backend. Backend should have its own forwarder.
      serverConnection.getConnection().write(new LoginPluginResponse(payload.getId(),responseData.isReadable(),responseData.retain()));
    });
  }

  public final VelocityLoginPayloadManager getPayloadManager() {
    return payloadManager;
  }

  public enum ClientPhase {
    VANILLA,
    HANDSHAKE,
    MODLIST,
    MODDED
  }
}
