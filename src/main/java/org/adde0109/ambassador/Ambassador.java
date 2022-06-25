package org.adde0109.ambassador;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Continuation;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PreLoginEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.LoginPhaseConnection;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;

import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;

import java.nio.file.Path;
import java.util.*;

@Plugin(id = "ambassador", name = "Ambassador", version = "0.1.0-SNAPSHOT", url = "", description = "", authors = {"adde0109"})
public class Ambassador {

  private final ProxyServer server;
  private final Logger logger;
  private final Path dataDirectory;
  private Optional<RegisteredServer> forgeServer;
  private AmbassadorConfig config;

  private static ForgeHandshakeDataHandler forgeHandshakeDataHandler;

  public Map<RegisteredServer, ForgeServerConnection> forgeServerConnectionMap;
  public Map<InetSocketAddress,ForgeConnection> incomingForgeConnections;

  @Inject
  public Ambassador(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory) {
    this.server = server;
    this.logger = logger;
    this.dataDirectory = dataDirectory;
  }

  @Subscribe
  public void onProxyInitialization(ProxyInitializeEvent event) {
    config = AmbassadorConfig.readOrCreateConfig(dataDirectory,server,logger);
    if(config != null) {
      forgeHandshakeDataHandler = new ForgeHandshakeDataHandler(logger,server);
      server.getEventManager().register(this, forgeHandshakeDataHandler);
    }
    else {
      logger.warn("Ambassador will be disabled because of errors");
    }

  }
  @Subscribe
  public void onPreLoginEvent(PreLoginEvent event, Continuation continuation) {
    if (!config.shouldHandle(event.getConnection().getProtocolVersion().getProtocol())) {
      continuation.resume();
      return;
    }
    RegisteredServer defaultServer = config.getServer(event.getConnection().getProtocolVersion().getProtocol());

    this.server.getEventManager().fire(new PreSyncEvent(event.getUsername(),event.getConnection(), defaultServer))
        .thenAccept((e) -> {
          if (e.getResult().getServer().isEmpty()) {
            //Do not sync
            return;
          }
          RegisteredServer newServer = e.getResult().getServer().get();

          ForgeConnection forgeConnection =  new ForgeConnection((LoginPhaseConnection) event.getConnection());

          //If a connection does not already exist, create one.
          if (!forgeServerConnectionMap.containsKey(newServer)) {
            forgeServerConnectionMap.put(newServer, new ForgeServerConnection(this,logger,newServer));
          }

          ForgeServerConnection forgeServerConnection = forgeServerConnectionMap.get(newServer);

          //Syncing
          forgeServerConnection.getHandshake().whenComplete((msg,ex) -> {
            if (ex != null) {
              logger.warn("Could not sync player '" + event.getUsername() + "' to server '"
                  + forgeServerConnection.getServerInfo().getName() +"' Cause: " + ex.getMessage());
            } else {
              forgeConnection.sendModlist(msg.modListPacket).thenAccept((response) -> {
                if (response != null) {
                  forgeServerConnection.setDefaultClientModlist(response);
                }
              });
              forgeConnection.sendOther(msg.otherPackets).thenAccept((response) -> {
                if (response != null) {
                  forgeServerConnection.setDefaultClientACK(response);
                }
                onSyncComplete(forgeConnection);
              });
            }
            //Writes the messages
            continuation.resume();
          });
        });
  }

  public void onSyncComplete(ForgeConnection forgeConnection) {
    if (forgeConnection.isModded()) {
      incomingForgeConnections.values().removeIf((c) -> !c.getConnection().isActive());
      incomingForgeConnections.put(forgeConnection.getConnection().getRemoteAddress(), forgeConnection);
    }
  }





}
