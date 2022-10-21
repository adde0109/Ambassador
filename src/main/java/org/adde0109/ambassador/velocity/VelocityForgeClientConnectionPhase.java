package org.adde0109.ambassador.velocity;

import com.velocitypowered.api.event.Continuation;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.connection.MinecraftConnection;
import com.velocitypowered.proxy.connection.backend.VelocityServerConnection;
import com.velocitypowered.proxy.connection.client.ClientConnectionPhase;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import com.velocitypowered.proxy.protocol.packet.LoginPluginMessage;
import com.velocitypowered.proxy.protocol.packet.LoginPluginResponse;
import org.adde0109.ambassador.forge.FML2CRPMClientConnectionPhase;

import java.util.concurrent.CompletableFuture;

public abstract class VelocityForgeClientConnectionPhase implements ClientConnectionPhase {
  //TODO:Make class when PCF is done


  VelocityLoginPayloadManager payloadManager;
  public VelocityForgeClientConnectionPhase.ClientPhase clientPhase = ClientPhase.HANDSHAKE;

  public ServerConnection internalServerConnection;
  public RegisteredServer forced;

  protected VelocityForgeClientConnectionPhase(ClientPhase clientPhase, VelocityLoginPayloadManager payloadManager) {
    this.clientPhase = clientPhase;
    this.payloadManager = payloadManager;
  }
  protected VelocityForgeClientConnectionPhase() {
  }

  public void handleLogin(ConnectedPlayer player, VelocityServer server, Continuation continuation) {
  }

  public CompletableFuture<Boolean> reset(VelocityServerConnection serverConnection, ConnectedPlayer player) {
    return CompletableFuture.completedFuture(false);
  }

  public void complete(VelocityServer server, ConnectedPlayer player, MinecraftConnection connection) {

  }

  final void fireLoginEvent(ConnectedPlayer player, VelocityServer server, Continuation continuation) {
    payloadManager = new VelocityLoginPayloadManager(player.getConnection());
    handleLogin(player,server,continuation);

    VelocityForgeHandshakeSessionHandler sessionHandler = new VelocityForgeHandshakeSessionHandler(player.getConnection().getSessionHandler(), player);
    player.getConnection().setSessionHandler(sessionHandler);
  }

  public void handleForward(VelocityServerConnection serverConnection, LoginPluginMessage payload) {
  }

  final public void forwardPayload(VelocityServerConnection serverConnection, LoginPluginMessage payload) {
    handleForward(serverConnection,payload);
    if (payloadManager == null) {
      return;
    }
    payloadManager.sendPayload("fml:loginwrapper",payload.content()).thenAccept((responseData) -> {
      //Move this to the backend. Backend should have its own forwarder.
      serverConnection.getConnection().write(new LoginPluginResponse(payload.getId(),responseData.isReadable(),responseData.retain()));
    });
    clientPhase = ClientPhase.MODLIST;
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
