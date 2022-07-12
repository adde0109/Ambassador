package org.adde0109.ambassador.forge;

import com.google.common.io.ByteArrayDataInput;
import com.velocitypowered.api.event.Continuation;
import com.velocitypowered.api.event.player.ServerLoginPluginMessageEvent;
import com.velocitypowered.api.proxy.LoginPhaseConnection;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import java.io.EOFException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class ForgeConnection {

  private final LoginPhaseConnection connection;

  private byte[] recivedClientModlist;
  private static byte[] recivedClientACK;

  private Optional<ForgeHandshakeUtils.CachedServerHandshake> transmittedHandshake = Optional.empty();
  private Optional<RegisteredServer> syncedTo = Optional.empty();


  public ForgeConnection(LoginPhaseConnection connection) {
    this.connection = connection;
  }

  public static CompletableFuture<Boolean> testIfForge(LoginPhaseConnection connection) {
    CompletableFuture<Boolean> future = new CompletableFuture<>();

    byte[] testPacket = ForgeHandshakeUtils.generateTestPacket();
    connection.sendLoginPluginMessage(MinecraftChannelIdentifier.create("fml", "loginwrapper"), testPacket,
        responseBody -> {
          future.complete(responseBody != null);
        });
    return future;
  }



  public CompletableFuture<Boolean> sync(ForgeServerConnection forgeServerConnection) {
    CompletableFuture<Boolean> future = new CompletableFuture<>();
    forgeServerConnection.getHandshake().whenComplete((msg,ex) -> {
      if (ex != null) {
        future.complete(false);
      }
        sendModlist(msg.modListPacket).thenAccept((response) -> {
          recivedClientModlist = response;
        });
        sendOther(msg.otherPackets).thenAccept((response) -> {
          ForgeConnection.recivedClientACK = response;
          transmittedHandshake = Optional.of(msg);
          syncedTo = Optional.of(forgeServerConnection.getServer());
        });
      future.complete(true);
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
      event.setResult(ServerLoginPluginMessageEvent.ResponseResult.reply(getRecivedClientModlist()));
    } else {
      event.setResult(ServerLoginPluginMessageEvent.ResponseResult.reply(getRecivedClientACK()));
    }
    continuation.resume();
}

  public LoginPhaseConnection getConnection() {
    return connection;
  }

  public Optional<ForgeHandshakeUtils.CachedServerHandshake> getTransmittedHandshake() {
    return transmittedHandshake;
  }

  public byte[] getRecivedClientModlist() {
    return recivedClientModlist;
  }

  public static byte[] getRecivedClientACK() {
    return recivedClientACK;
  }
}
