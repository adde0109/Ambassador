package org.adde0109.ambassador.event;

import com.velocitypowered.api.event.ResultedEvent;
import com.velocitypowered.api.event.player.ServerPreConnectEvent;
import com.velocitypowered.api.proxy.InboundConnection;
import com.velocitypowered.api.proxy.server.RegisteredServer;

public class PreSyncEvent implements ResultedEvent<ServerPreConnectEvent.ServerResult> {

  private final String username;
  private final InboundConnection connection;
  private final RegisteredServer originalServer;
  private ServerPreConnectEvent.ServerResult result;

  public PreSyncEvent(String username, InboundConnection connection,
                      RegisteredServer originalServer) {
    this.username = username;
    this.connection = connection;
    this.originalServer = originalServer;
    this.result = ServerPreConnectEvent.ServerResult.allowed(originalServer);
  }

  @Override
  public ServerPreConnectEvent.ServerResult getResult() {
    return result;
  }

  @Override
  public void setResult(ServerPreConnectEvent.ServerResult result) {

  }
}
