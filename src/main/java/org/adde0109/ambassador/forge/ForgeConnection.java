package org.adde0109.ambassador.forge;

import com.velocitypowered.api.event.Continuation;
import com.velocitypowered.api.proxy.LoginPhaseConnection;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class ForgeConnection {

  private final LoginPhaseConnection connection;

  private byte[] recivedClientModlist;
  private static byte[] recivedClientACK;

  private ForgeHandshakeUtils.CachedServerHandshake transmittedHandshake;


  private ForgeConnection(LoginPhaseConnection connection) {
    this.connection = connection;
  }


  public static CompletableFuture<ForgeConnection> sync(LoginPhaseConnection connection, ForgeServerConnection forgeServerConnection, Continuation continuation) {
    CompletableFuture<ForgeConnection> future = new CompletableFuture<>();
    ForgeConnection forgeConnection = new ForgeConnection(connection);
    forgeServerConnection.getHandshake().whenComplete((msg,ex) -> {
      if (ex != null) {
        future.completeExceptionally(ex);
      } else {
        forgeConnection.sendModlist(msg.modListPacket).thenAccept((response) -> {
          if (response != null) {
            forgeServerConnection.setDefaultClientModlist(response);
            future.complete(forgeConnection);
          } else {
            future.complete(null);
          }
        });
        forgeConnection.sendOther(msg.otherPackets).thenAccept((response) -> {
          if (response != null) {
            forgeServerConnection.setDefaultClientACK(response);
            future.complete(forgeConnection);
          } else {
            future.complete(null);
          }
        });
        forgeConnection.transmittedHandshake = msg;
      }
      //Write
      continuation.resume();
    });
    return future;
  }

  public CompletableFuture<byte[]> sendModlist(byte[] modListPacket) {
    CompletableFuture<byte[]> future = new CompletableFuture<>();
    connection.sendLoginPluginMessage(MinecraftChannelIdentifier.create("fml","loginwrapper"), modListPacket,
        responseBody -> {
      recivedClientModlist = responseBody;
      future.complete(recivedClientModlist);
        });
    return future;
  }

  CompletableFuture<byte[]> sendOther(List<byte[]> otherPackets) {
    CompletableFuture<byte[]> future = new CompletableFuture<>();
    for (int i = 0;i<otherPackets.size();i++) {
      connection.sendLoginPluginMessage(MinecraftChannelIdentifier.create("fml","loginwrapper"), otherPackets.get(i),
          (i<(otherPackets.size()-1)) ? responseBody -> {} : responseBody -> {
        recivedClientACK = responseBody;
        future.complete(responseBody);
          });
    }
    return future;
  }

  public LoginPhaseConnection getConnection() {
    return connection;
  }

  public ForgeHandshakeUtils.CachedServerHandshake getTransmittedHandshake() {
    return transmittedHandshake;
  }

  public byte[] getRecivedClientModlist() {
    return recivedClientModlist;
  }

  public static byte[] getRecivedClientACK() {
    return recivedClientACK;
  }
}
