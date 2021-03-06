package org.adde0109.ambassador.forge;

import com.google.common.io.ByteArrayDataInput;
import com.velocitypowered.api.event.Continuation;
import com.velocitypowered.api.event.player.ServerLoginPluginMessageEvent;
import com.velocitypowered.api.proxy.LoginPhaseConnection;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import org.slf4j.Logger;

import java.io.EOFException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class ForgeConnection {

  private final Logger logger;
  private final LoginPhaseConnection connection;

  private Optional<byte[]> recivedClientModlist = Optional.empty();
  private static byte[] recivedClientACK;
  private boolean ignoreSyncExepction = false;

  private Optional<ForgeHandshakeUtils.CachedServerHandshake> transmittedHandshake = Optional.empty();
  private Optional<RegisteredServer> syncedTo = Optional.empty();


  public ForgeConnection(LoginPhaseConnection connection, Logger logger) {
    this.connection = connection;
    this.logger = logger;
  }

  public CompletableFuture<Boolean> testIfForge(LoginPhaseConnection connection) {
    CompletableFuture<Boolean> future = new CompletableFuture<>();

    byte[] testPacket = ForgeHandshakeUtils.generateTestPacket();
    connection.sendLoginPluginMessage(MinecraftChannelIdentifier.create("fml", "loginwrapper"), testPacket,
        responseBody -> {
          future.complete(responseBody != null);
          ignoreSyncExepction = responseBody == null;
        });
    return future;
  }



  public CompletableFuture<Boolean> sync(ForgeServerConnection forgeServerConnection) {
    CompletableFuture<Boolean> future = new CompletableFuture<>();
    forgeServerConnection.getHandshake().whenComplete((msg,ex) -> {
      if (ex != null) {
        future.complete(false);
        logger.warn("Sync Exception: " + ex);
      } else {
        sendModlist(msg.modListPacket).thenAccept((response) -> {
          if (!ignoreSyncExepction && response == null) {
            logger.warn("Sync Exception: Client responded with an empty body.");
          }
          recivedClientModlist = Optional.ofNullable(response);
        });
        sendOther(msg.otherPackets).thenAccept((response) -> {
          if (!ignoreSyncExepction && response == null) {
            logger.warn("Sync Exception: Client responded with an empty body.");
          }
          ForgeConnection.recivedClientACK = response;
          transmittedHandshake = Optional.of(msg);
          syncedTo = Optional.of(forgeServerConnection.getServer());
        });
        future.complete(true);
      }
    });
    return future;
  }

  private CompletableFuture<byte[]> sendModlist(byte[] modListPacket) {
    CompletableFuture<byte[]> future = new CompletableFuture<>();
    connection.sendLoginPluginMessage(MinecraftChannelIdentifier.create("fml", "loginwrapper"), modListPacket,
        future::complete);
    return future;
  }

  private CompletableFuture<byte[]> sendOther(List<byte[]> otherPackets) {
    CompletableFuture<byte[]> future = new CompletableFuture<>();
    for (int i = 0; i < otherPackets.size(); i++) {
      connection.sendLoginPluginMessage(MinecraftChannelIdentifier.create("fml", "loginwrapper"), otherPackets.get(i),
          (i < (otherPackets.size() - 1)) ? responseBody -> {
          } : future::complete);
    }
    return future;
  }

  public void handleServerHandshakePacket(ServerLoginPluginMessageEvent event, Continuation continuation) {
    ByteArrayDataInput data = event.contentsAsDataStream();
    if (data.skipBytes(14) != 14) {  //Channel Identifier
      continuation.resumeWithException(new EOFException());
      return;
    }
    ForgeHandshakeUtils.readVarInt(data); //Length
    int packetID = ForgeHandshakeUtils.readVarInt(data);

    if (packetID == 1) {
      if (getRecivedClientModlist().isPresent()) {
        event.setResult(ServerLoginPluginMessageEvent.ResponseResult.reply(getRecivedClientModlist().get()));
      } else {
        continuation.resumeWithException(new Exception("Client isn't synced. This should have been caught" +
                " during serverPreConnect"));
        return;
      }
    } else {
      if (getRecivedClientACK() != null) {
        event.setResult(ServerLoginPluginMessageEvent.ResponseResult.reply(getRecivedClientACK()));
      } else {
        continuation.resumeWithException(new Exception("No response available."));
        return;
      }
    }
    continuation.resume();
}

  public LoginPhaseConnection getConnection() {
    return connection;
  }

  public Optional<ForgeHandshakeUtils.CachedServerHandshake> getTransmittedHandshake() {
    return transmittedHandshake;
  }

  public Optional<byte[]> getRecivedClientModlist() {
    return recivedClientModlist;
  }

  public static byte[] getRecivedClientACK() {
    return recivedClientACK;
  }
}
