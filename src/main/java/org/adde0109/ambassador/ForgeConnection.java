package org.adde0109.ambassador;

import com.velocitypowered.api.proxy.InboundConnection;
import com.velocitypowered.api.proxy.LoginPhaseConnection;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class ForgeConnection {

  private final LoginPhaseConnection connection;

  private byte[] recivedClientModlist;

  private boolean isModded = false;

  ForgeConnection(LoginPhaseConnection connection) {
    this.connection = connection;
  }

  public CompletableFuture<byte[]> sendModlist(byte[] modListPacket) {
    CompletableFuture<byte[]> future = new CompletableFuture<byte[]>();
    connection.sendLoginPluginMessage(MinecraftChannelIdentifier.create("fml","loginwrapper"), modListPacket,
        responseBody -> {
      if (responseBody != null) {
        isModded = true;
      }
      recivedClientModlist = responseBody;
      future.complete(recivedClientModlist);
        });
    return future;
  }

  CompletableFuture<byte[]> sendOther(List<byte[]> otherPackets) {
    CompletableFuture<byte[]> future = new CompletableFuture<byte[]>();
    for (int i = 0;i<otherPackets.size();i++) {
      connection.sendLoginPluginMessage(MinecraftChannelIdentifier.create("fml","loginwrapper"), otherPackets.get(i),
          (i<(otherPackets.size()-1)) ? responseBody -> {} : future::complete);
    }
    return future;
  }

  public boolean isModded() {
    return isModded;
  }
  public LoginPhaseConnection getConnection() {
    return connection;
  }

}
