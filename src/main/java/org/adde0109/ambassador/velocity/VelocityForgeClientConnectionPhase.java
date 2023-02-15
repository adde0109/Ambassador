package org.adde0109.ambassador.velocity;

import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.connection.MinecraftConnection;
import com.velocitypowered.proxy.connection.backend.VelocityServerConnection;
import com.velocitypowered.proxy.connection.client.ClientConnectionPhase;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import com.velocitypowered.proxy.protocol.packet.LoginPluginMessage;
import com.velocitypowered.proxy.protocol.packet.LoginPluginResponse;

import java.lang.reflect.InvocationTargetException;
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

  public RegisteredServer chooseServer(ConnectedPlayer player) {
    return null;
  }

  public CompletableFuture<Boolean> reset(RegisteredServer server, ConnectedPlayer player) {
    return CompletableFuture.completedFuture(false);
  }



  final void onFirstLogin(ConnectedPlayer player, VelocityServer server) throws InterruptedException, InvocationTargetException, IllegalAccessException {
  }

  public void handleForward(VelocityServerConnection serverConnection, LoginPluginMessage payload) {
  }

  public final VelocityLoginPayloadManager getPayloadManager() {
    return payloadManager;
  }

  public boolean handle(ConnectedPlayer player, LoginPluginResponse response, VelocityServerConnection server) {
    server.getConnection().write(response.retain());
    return true;
  }

  public enum ClientPhase {
    VANILLA,
    HANDSHAKE,
    MODLIST,
    MODDED
  }
}
