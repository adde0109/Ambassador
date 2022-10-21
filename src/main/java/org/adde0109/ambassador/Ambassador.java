package org.adde0109.ambassador;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;

import java.lang.reflect.Field;
import java.util.concurrent.Callable;

import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.network.ConnectionManager;
import io.netty.channel.ChannelInitializer;
import org.adde0109.ambassador.velocity.VelocityBackendChannelInitializer;
import org.adde0109.ambassador.velocity.VelocityServerChannelInitializer;
import org.adde0109.ambassador.velocity.VelocityEventHandler;
import org.bstats.charts.SingleLineChart;
import org.bstats.velocity.Metrics;
import org.slf4j.Logger;

import java.nio.file.Path;

@Plugin(id = "ambassador", name = "Ambassador", version = "1.0.9-alpha", authors = {"adde0109"})
public class Ambassador {

  public ProxyServer server;
  public final Logger logger;
  private final Metrics.Factory metricsFactory;
  private final Path dataDirectory;




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

    server.getEventManager().register(this, new VelocityEventHandler(this));

    inject();
  }

  private void inject() throws ReflectiveOperationException {
    Field cmField = VelocityServer.class.getDeclaredField("cm");
    cmField.setAccessible(true);

    ChannelInitializer<?> original = ((ConnectionManager) cmField.get(server)).serverChannelInitializer.get();
    ((ConnectionManager) cmField.get(server)).serverChannelInitializer.set(new VelocityServerChannelInitializer(original));

    ChannelInitializer<?> originalBackend = ((ConnectionManager) cmField.get(server)).backendChannelInitializer.get();
    ((ConnectionManager) cmField.get(server)).backendChannelInitializer.set(new VelocityBackendChannelInitializer(originalBackend,(VelocityServer) server));
  }

  private void initMetrics() {
    Metrics metrics = metricsFactory.make(this, 15655);
  }
}
