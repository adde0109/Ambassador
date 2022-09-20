package org.adde0109.ambassador;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Continuation;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.PlayerChooseInitialServerEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyReloadEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;

import java.lang.reflect.Field;
import java.util.concurrent.Callable;

import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.network.ConnectionManager;
import io.netty.channel.ChannelInitializer;
import org.adde0109.ambassador.forge.ForgeConnection;
import org.adde0109.ambassador.forge.ForgeHandshakeHandler;
import org.adde0109.ambassador.forge.ForgeHandshakeUtils;
import org.adde0109.ambassador.forge.ForgeServerSwitchHandler;
import org.adde0109.ambassador.velocity.VelocityServerChannelInitializer;
import org.adde0109.ambassador.velocity.VelocityEventHandler;
import org.bstats.charts.SingleLineChart;
import org.bstats.velocity.Metrics;
import org.slf4j.Logger;

import java.nio.file.Path;

@Plugin(id = "ambassador", name = "Ambassador", version = "1.0.0-alpha", authors = {"adde0109"})
public class Ambassador {

  public ProxyServer server;
  public final Logger logger;
  public AmbassadorConfig config;
  private final Metrics.Factory metricsFactory;
  private final Path dataDirectory;

  public ForgeHandshakeHandler forgeHandshakeHandler;
  public ForgeServerSwitchHandler forgeServerSwitchHandler;



  @Inject
  public Ambassador(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory, Metrics.Factory metricsFactory) {
    this.server = server;
    this.logger = logger;
    this.dataDirectory = dataDirectory;
    this.metricsFactory = metricsFactory;
  }

  @Subscribe
  public void onProxyInitialization(ProxyInitializeEvent event) throws ReflectiveOperationException {
    initMetrics();

    config = AmbassadorConfig.readOrCreateConfig(dataDirectory,server,logger);
    if(config != null) {
      forgeHandshakeHandler = new ForgeHandshakeHandler(this);
      forgeServerSwitchHandler = new ForgeServerSwitchHandler(this);
      server.getEventManager().register(this, new VelocityEventHandler(this));
      server.getEventManager().register(this,forgeServerSwitchHandler);
    }
    else {
      logger.warn("Ambassador will be disabled because of errors");
    }

    ForgeHandshakeUtils.HandshakeReceiver.logger = logger;
    inject();
  }

  private void inject() throws ReflectiveOperationException {
    Field cmField = VelocityServer.class.getDeclaredField("cm");
    cmField.setAccessible(true);
    ChannelInitializer<?> original = ((ConnectionManager) cmField.get(server)).serverChannelInitializer.get();
    ((ConnectionManager) cmField.get(server)).serverChannelInitializer.set(new VelocityServerChannelInitializer(original));
  }

  @Subscribe
  public void onProxyReloadEvent(ProxyReloadEvent event) {
    AmbassadorConfig c = AmbassadorConfig.readOrCreateConfig(dataDirectory,server,logger);
    if (config != null) {
      config = c;
      logger.info("Successfully reloaded the config");
    } else {
      logger.warn("Using the old config");
    }
  }


  @Subscribe
  public void onPlayerChooseInitialServerEvent(PlayerChooseInitialServerEvent event, Continuation continuation) {
    //Only handle Forge connections
    if((event.getInitialServer().isPresent()) && (forgeHandshakeHandler.getForgeConnection(event.getPlayer()).isPresent())) {
      //Forge client
      ForgeConnection forgeConnection = forgeHandshakeHandler.getForgeConnection(event.getPlayer()).get();
      if (forgeConnection.isForced()) {
        event.setInitialServer(forgeConnection.getSyncedServer().get());
      }
      forgeConnection.setForced(config.getForced(forgeConnection.getConnection().getProtocolVersion().getProtocol()));
    }
    continuation.resume();
  }

  private void initMetrics() {
    Metrics metrics = metricsFactory.make(this, 15655);
    metrics.addCustomChart(new SingleLineChart("modern_forge_players", new Callable<Integer>() {
      @Override
      public Integer call() throws Exception {
        return (forgeHandshakeHandler != null) ? forgeHandshakeHandler.getAmountOfForgeConnections() : 0;
      }
    }));
  }
}
