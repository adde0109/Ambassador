package org.adde0109.ambassador.forge;

import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerInfo;
import java.util.concurrent.CompletableFuture;
import org.slf4j.Logger;

public class ForgeServerConnection {

  private static final int PACKET_LENGTH_INDEX = 14;    //length of "fml:handshake"+1
  private final RegisteredServer handshakeServer;
  private ForgeHandshakeUtils.CachedServerHandshake handshake;

  public RegisteredServer getServer() {
    return handshakeServer;
  }

  public ForgeServerConnection(RegisteredServer handshakeServer) {
    this.handshakeServer = handshakeServer;
  }

  public CompletableFuture<ForgeHandshakeUtils.CachedServerHandshake> getHandshake() {
    CompletableFuture<ForgeHandshakeUtils.CachedServerHandshake> future;
    if (handshake == null) {
      future = ForgeHandshakeUtils.HandshakeReceiver.downloadHandshake(handshakeServer);
    } else {
      future = ForgeHandshakeUtils.HandshakeReceiver.downloadHandshake(handshakeServer,handshake);
    }
    future.thenAccept(p -> handshake = p);
    return future;
  }

  public ServerInfo getServerInfo() {
    return handshakeServer.getServerInfo();
  }

}
