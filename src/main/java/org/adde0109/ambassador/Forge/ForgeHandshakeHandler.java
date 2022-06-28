package org.adde0109.ambassador.Forge;

import com.velocitypowered.api.event.Continuation;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PreLoginEvent;
import com.velocitypowered.api.event.player.ServerLoginPluginMessageEvent;
import com.velocitypowered.api.proxy.LoginPhaseConnection;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.adde0109.ambassador.AmbassadorConfig;
import org.slf4j.Logger;

public class ForgeHandshakeHandler {

  private final AmbassadorConfig config;
  private final ProxyServer server;
  private final Logger logger;

  private Map<RegisteredServer, ForgeServerConnection>
      forgeServerConnectionMap = new HashMap<RegisteredServer,ForgeServerConnection>();
  private Map<InetSocketAddress,ForgeConnection> incomingForgeConnections = new HashMap<InetSocketAddress,ForgeConnection>();


  public ForgeHandshakeHandler(AmbassadorConfig config, ProxyServer server, Logger logger) {
    this.config = config;
    this.server = server;
    this.logger = logger;
  }

  @Subscribe
  public void onPreLoginEvent(PreLoginEvent event, Continuation continuation) {
    if (!config.shouldHandle(event.getConnection().getProtocolVersion().getProtocol())) {
      continuation.resume();
      return;
    }
    RegisteredServer defaultServer = config.getServer(event.getConnection().getProtocolVersion().getProtocol());

          if (defaultServer == null) {
            continuation.resume();
            return;
          }



          //If a connection does not already exist, create one.
          if (!forgeServerConnectionMap.containsKey(defaultServer)) {
            forgeServerConnectionMap.put(defaultServer, new ForgeServerConnection(defaultServer,logger));
          }

          ForgeServerConnection forgeServerConnection = forgeServerConnectionMap.get(defaultServer);

          //Syncing - continuation is forwarded to this method
          ForgeConnection.sync((LoginPhaseConnection) event.getConnection(),forgeServerConnection,continuation).thenAccept(
              this::onSyncComplete);
  }

  private void onSyncComplete(ForgeConnection forgeConnection) {
    if (forgeConnection != null) {
      incomingForgeConnections.values().removeIf((c) -> !c.getConnection().isActive());
      incomingForgeConnections.put(forgeConnection.getConnection().getRemoteAddress(), forgeConnection);
    }
  }

  public Optional<ForgeConnection> getForgeConnection(Player player) {
    return getForgeConnection(player.getRemoteAddress());
  }

  private Optional<ForgeConnection> getForgeConnection(InetSocketAddress socketAddress) {
    incomingForgeConnections.values().removeIf((c) -> !c.getConnection().isActive());
    return Optional.ofNullable(incomingForgeConnections.get(socketAddress));
  }

  public Optional<ForgeServerConnection> getForgeServerConnection(RegisteredServer registeredServer) {
    return Optional.ofNullable(forgeServerConnectionMap.get(registeredServer));
  }

  public void registerForgeServer(RegisteredServer server, ForgeServerConnection forgeServerConnection) {
    forgeServerConnectionMap.put(server,forgeServerConnection);
  }

  @Subscribe
  public void onServerLoginPluginMessageEvent(ServerLoginPluginMessageEvent event, Continuation continuation) {
    //Only respond the servers that we can respond to
    if((!forgeServerConnectionMap.containsKey(event.getConnection().getServer())
        || (getForgeConnection(event.getConnection().getPlayer()).isEmpty()))) {
      continuation.resume();
      return;
    }
    //Grab the connection responsible for this - no pun intended
    ForgeServerConnection connection = forgeServerConnectionMap.get(event.getConnection().getServer());
    connection.handle(event,continuation);
  }
}
