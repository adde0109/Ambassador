package org.adde0109.ambassador;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Continuation;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.ServerPreConnectEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;

import org.adde0109.ambassador.Forge.ForgeConnection;
import org.adde0109.ambassador.Forge.ForgeHandshakeHandler;
import org.adde0109.ambassador.Forge.ForgeServerConnection;
import org.checkerframework.checker.index.qual.PolyUpperBound;
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

  private ForgeHandshakeHandler forgeHandshakeHandler;



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
      forgeHandshakeHandler = new ForgeHandshakeHandler(config, server, logger);
      server.getEventManager().register(this, forgeHandshakeHandler);
    }
    else {
      logger.warn("Ambassador will be disabled because of errors");
    }

  }

  @Subscribe
  public void onServerPreConnectEvent(ServerPreConnectEvent event, Continuation continuation) {
    Optional<ForgeConnection> forgeConnection = forgeHandshakeHandler.getForgeConnection(event.getPlayer());
    Optional<ForgeServerConnection> forgeServerConnectionOptional = forgeHandshakeHandler.getForgeServerConnection(event.getOriginalServer());
    if (forgeConnection.isPresent()) {
      ForgeServerConnection forgeServerConnection;
      if (forgeServerConnectionOptional.isEmpty()) {
        forgeServerConnection = new ForgeServerConnection(event.getOriginalServer(), logger);
      } else {
        forgeServerConnection = forgeServerConnectionOptional.get();
      }
      forgeServerConnection.getHandshake().whenComplete((msg, ex) -> {
        if (ex != null) {
          continuation.resume();
        } else {
          if (Arrays.equals(msg.modListPacket,forgeConnection.get().getTransmittedHandshake().modListPacket)) {
            continuation.resume();
          } else {
            event.setResult(ServerPreConnectEvent.ServerResult.denied());
            logger.warn("Resync needed");
            continuation.resume();
          }
        }
        if (forgeServerConnectionOptional.isEmpty()) {
          forgeServerConnection.setDefaultClientModlist(forgeConnection.get().getRecivedClientModlist());
          forgeServerConnection.setDefaultClientACK(ForgeConnection.getRecivedClientACK());
          forgeHandshakeHandler.registerForgeServer(event.getOriginalServer(), forgeServerConnection);
        }

      });
    } else {
      continuation.resume();
    }
  }

}
