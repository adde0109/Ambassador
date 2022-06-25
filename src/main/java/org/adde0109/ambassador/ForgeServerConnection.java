package org.adde0109.ambassador;

import com.google.common.io.ByteArrayDataInput;
import com.velocitypowered.api.event.Continuation;
import com.velocitypowered.api.event.player.ServerLoginPluginMessageEvent;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerInfo;
import java.io.EOFException;
import java.util.concurrent.CompletableFuture;
import org.slf4j.Logger;

public class ForgeServerConnection {

  private static final int PACKET_LENGTH_INDEX = 14;    //length of "fml:handshake"+1
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

  public void handle(ServerLoginPluginMessageEvent event, Continuation continuation) {
    ByteArrayDataInput data = event.contentsAsDataStream();
    if(data.skipBytes(PACKET_LENGTH_INDEX) != PACKET_LENGTH_INDEX) {  //Channel Identifier
      continuation.resumeWithException(new EOFException());
      return;
    }
    ForgeHandshakeDataHandler.readVarInt(data); //Length
    int packetID = ForgeHandshakeDataHandler.readVarInt(data);

    if(packetID == 1) {
      event.setResult(ServerLoginPluginMessageEvent.ResponseResult.reply(defaultClientModlist));
    }
    else {
      event.setResult(ServerLoginPluginMessageEvent.ResponseResult.reply(defaultClientACK));
    }
    continuation.resume();
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
