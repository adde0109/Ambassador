package org.adde0109.ambassador;

import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerInfo;
import java.util.concurrent.CompletableFuture;
import org.slf4j.Logger;

public class ForgeServerConnection {

  private final Ambassador ambassador;
  private final Logger logger;
  private final RegisteredServer handshakeServer;

  private ForgeHandshakeDataHandler.CachedServerHandshake handshake;

  private byte[] defaultClientModlist;
  private byte[] defaultClientACK;

  public RegisteredServer getServer() {
    return handshakeServer;
  }

  public ForgeServerConnection(Ambassador ambassador, Logger logger, RegisteredServer handshakeServer) {
    this.ambassador = ambassador;
    this.logger = logger;
    this.handshakeServer = handshakeServer;
  }

  public CompletableFuture<ForgeHandshakeDataHandler.CachedServerHandshake> getHandshake() {
    CompletableFuture<ForgeHandshakeDataHandler.CachedServerHandshake> future;
    if (handshakeServer.getPlayersConnected().isEmpty() || (handshake == null)) {
      ForgeHandshakeDataHandler.handshakeReceiver
          receiver = new ForgeHandshakeDataHandler.handshakeReceiver(handshakeServer, logger);
      future = receiver.downloadHandshake();
      future.thenAccept(p -> {
        handshake = p;
      });
      return future;
    } else {
      future = new CompletableFuture<>();
      future.complete(handshake);
      return future;
    }
  }

  public void setDefaultClientModlist(byte[] modlist) {
    this.defaultClientModlist = modlist;
  }

  public void setDefaultClientACK(byte[] ACK) {
    this.defaultClientACK = ACK;
  }

  public ServerInfo getServerInfo() {
    return handshakeServer.getServerInfo();
  }

}
