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

  private boolean ignoreSyncExepction = false;

  private boolean forced = false;

  private SyncResult syncResult;


  public ForgeConnection(LoginPhaseConnection connection, Logger logger) {
    this.connection = connection;
    this.logger = logger;
  }

  public CompletableFuture<Boolean> testIfForge(LoginPhaseConnection connection) {
    CompletableFuture<Boolean> future = new CompletableFuture<>();

    byte[] testPacket = ForgeHandshakeUtils.generateTestPacket();
    //This gets also sent to vanilla
    connection.sendLoginPluginMessage(MinecraftChannelIdentifier.create("fml", "loginwrapper"), testPacket,
        responseBody -> {
          future.complete(responseBody != null);
          ignoreSyncExepction = responseBody == null;
        });
    return future;
  }



  public void startSync(ForgeServerConnection forgeServerConnection, Continuation continuation) {
    forgeServerConnection.getHandshake().whenComplete((msg,ex) -> {
      if (ex != null) {
        continuation.resume();
        logger.warn("Sync Exception: " + ex);
      } else {
        CompletableFuture<byte[]> clientModListFuture = new CompletableFuture<>();
        //This gets also sent to vanilla
        sendModlist(msg.modListPacket).thenAccept((response) -> {
          if (!ignoreSyncExepction && response == null) {
            logger.warn("Sync Exception: Client responded with an empty body.");
          }
          clientModListFuture.complete(response);
        });
        syncResult = new SyncResult(msg,clientModListFuture, forgeServerConnection.getServer());
        //This gets also sent to vanilla
        sendOther(msg.otherPackets).thenAccept((response) -> {
          if (!ignoreSyncExepction && response == null) {
            logger.warn("Sync Exception: Client responded with an empty body.");
          }
          //TODO: Generate the ACK packet ourself.
          if (response != null && SyncResult.recivedClientACK == null) {
            SyncResult.recivedClientACK = response;
          }
          syncResult.complete(syncResult);
        });
        continuation.resume();
      }
    });
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
    if (getSyncResult().isEmpty()) {
      continuation.resumeWithException(new Exception("Client isn't synced. This should have been caught" +
              " during serverPreConnect"));
      return;
    }
    ByteArrayDataInput data = event.contentsAsDataStream();
    if (data.skipBytes(14) != 14) {  //Channel Identifier
      continuation.resumeWithException(new EOFException());
      return;
    }
    ForgeHandshakeUtils.readVarInt(data); //Length
    int packetID = ForgeHandshakeUtils.readVarInt(data);

    if (packetID == 1) {
      getSyncResult().get().getRecivedClientModlist().whenComplete((msg,ex) -> {
        event.setResult(ServerLoginPluginMessageEvent.ResponseResult.reply(msg));
        continuation.resume();
      });
    } else {
      if (SyncResult.recivedClientACK != null) {
        event.setResult(ServerLoginPluginMessageEvent.ResponseResult.reply(SyncResult.recivedClientACK));
      } else {
        continuation.resumeWithException(new Exception("No ACK response packet available."));
        return;
      }
      continuation.resume();
    }
}

  public LoginPhaseConnection getConnection() {
    return connection;
  }

  public Optional<SyncResult> getSyncResult() {
    return Optional.ofNullable(syncResult);
  }
  public void setForced(boolean forced) {
    this.forced = forced;
  }
  public boolean isForced() {
    return forced;
  }
  public static class SyncResult extends CompletableFuture<SyncResult> {
    private final ForgeHandshakeUtils.CachedServerHandshake transmittedHandshake;
    private final CompletableFuture<byte[]> recivedClientModlist;
    private static byte[] recivedClientACK;
    private final RegisteredServer syncedTo;

    SyncResult(ForgeHandshakeUtils.CachedServerHandshake transmittedHandshake, CompletableFuture<byte[]> recivedClientModlist, RegisteredServer syncedTo) {
      this.transmittedHandshake = transmittedHandshake;
      this.recivedClientModlist = recivedClientModlist;
      this.syncedTo = syncedTo;
    }

    public ForgeHandshakeUtils.CachedServerHandshake getTransmittedHandshake() {
      return transmittedHandshake;
    }

    public CompletableFuture<byte[]> getRecivedClientModlist() {
      return recivedClientModlist;
    }

    public RegisteredServer getSyncedServer() {
      return syncedTo;
    }
  }
}
