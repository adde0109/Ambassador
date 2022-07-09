package org.adde0109.ambassador;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Continuation;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.KickedFromServerEvent;
import com.velocitypowered.api.event.player.PlayerChooseInitialServerEvent;
import com.velocitypowered.api.event.player.ServerPreConnectEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyPingEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;

import com.velocitypowered.api.proxy.server.ServerPing;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.adde0109.ambassador.forge.ForgeConnection;
import org.adde0109.ambassador.forge.ForgeHandshakeHandler;
import org.adde0109.ambassador.forge.ForgeHandshakeUtils;
import org.adde0109.ambassador.forge.ForgeServerConnection;
import org.bstats.velocity.Metrics;
import org.slf4j.Logger;

import java.nio.file.Path;
import java.util.*;

@Plugin(id = "ambassador", name = "Ambassador", version = "0.2.1-SNAPSHOT", authors = {"adde0109"})
public class Ambassador {

  private final ProxyServer server;
  private final Logger logger;
  private final Metrics.Factory metricsFactory;
  private final Path dataDirectory;
  private AmbassadorConfig config;

  private ForgeHandshakeHandler forgeHandshakeHandler;



  @Inject
  public Ambassador(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory, Metrics.Factory metricsFactory) {
    this.server = server;
    this.logger = logger;
    this.dataDirectory = dataDirectory;
    this.metricsFactory = metricsFactory;
  }

  @Subscribe
  public void onProxyInitialization(ProxyInitializeEvent event) {
    metricsFactory.make(this, 15655);

    config = AmbassadorConfig.readOrCreateConfig(dataDirectory,server,logger);
    if(config != null) {
      forgeHandshakeHandler = new ForgeHandshakeHandler(config, server, logger);
      server.getEventManager().register(this, forgeHandshakeHandler);
    }
    else {
      logger.warn("Ambassador will be disabled because of errors");
    }

    ForgeHandshakeUtils.HandshakeReceiver.logger = logger;
  }

  @Subscribe
  public void onServerPreConnectEvent(ServerPreConnectEvent event, Continuation continuation) {
    Optional<ForgeServerConnection> forgeServerConnectionOptional = forgeHandshakeHandler.getForgeServerConnection(event.getOriginalServer());
    if (forgeServerConnectionOptional.isPresent()) {
      //Check 1; Check if the server is already known to us. Check if the client is compatible.
      ForgeServerConnection forgeServerConnection = forgeServerConnectionOptional.get();
      forgeServerConnection.getHandshake().whenComplete((msg, ex) -> {
        if (ex != null) {
          //The server was forge but aren't right now. Or it's just offline.
          continuation.resume();
        } else {
          Optional<ForgeConnection> forgeConnection = forgeHandshakeHandler.getForgeConnection(event.getPlayer());
          if (forgeConnection.isEmpty() && (event.getPlayer().getCurrentServer().isPresent())) {
            //If vanilla tries to connect to a server we know is forge
            event.setResult(ServerPreConnectEvent.ServerResult.denied());
            event.getPlayer().sendMessage(Component.text("This server requires Forge!", NamedTextColor.RED));
            continuation.resume();
          } else if (forgeConnection.isPresent()) {
            if (msg.equals(forgeConnection.get().getTransmittedHandshake())) {
              //The client's registry is the same as the server's
              continuation.resume();
            } else {
              event.setResult(ServerPreConnectEvent.ServerResult.denied());
              logger.warn("Resync needed");
              continuation.resume();
            }
          } else {
            //If the initial server is forge while the client is vanilla.
            //Can't handle, just let it pass.
            continuation.resume();
          }
        }
      });
    } else {
      //The server is not known to us.
      continuation.resume();
    }
  }

  @Subscribe
  public void onPlayerChooseInitialServerEvent(PlayerChooseInitialServerEvent event, Continuation continuation) {
    //Only handle Forge connections
    if((event.getInitialServer().isPresent()) && (forgeHandshakeHandler.getForgeConnection(event.getPlayer()).isPresent())) {
      //Forge client
      ForgeConnection forgeConnection = forgeHandshakeHandler.getForgeConnection(event.getPlayer()).get();
      if (config.getForced(forgeConnection.getConnection().getProtocolVersion().getProtocol())) {
        event.setInitialServer(config.getServer(forgeConnection.getConnection().getProtocolVersion().getProtocol()));
      }
    }
    continuation.resume();
  }
}
