package org.adde0109.ambassador;

import com.google.inject.Inject;
import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyReloadEvent;
import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.network.BackendChannelInitializerHolder;
import com.velocitypowered.proxy.network.ConnectionManager;
import com.velocitypowered.proxy.network.ServerChannelInitializerHolder;
import com.velocitypowered.proxy.protocol.packet.brigadier.ArgumentIdentifier;
import com.velocitypowered.proxy.protocol.packet.brigadier.ArgumentPropertyRegistry;
import com.velocitypowered.proxy.protocol.packet.brigadier.ArgumentPropertySerializer;
import io.netty.buffer.ByteBuf;
import org.adde0109.ambassador.velocity.VelocityBackendChannelInitializer;
import org.adde0109.ambassador.velocity.VelocityServerChannelInitializer;
import org.adde0109.ambassador.velocity.VelocityEventHandler;
import org.adde0109.ambassador.velocity.protocol.EnumArgumentProperty;
import org.adde0109.ambassador.velocity.protocol.EnumArgumentPropertySerializer;
import org.adde0109.ambassador.velocity.protocol.ModIdArgumentProperty;
import org.bstats.velocity.Metrics;
import org.slf4j.Logger;

import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import static com.velocitypowered.api.network.ProtocolVersion.MINECRAFT_1_19;
import static com.velocitypowered.proxy.protocol.packet.brigadier.ArgumentIdentifier.mapSet;

@Plugin(id = "ambassador", name = "Ambassador", version = "1.4.0", authors = {"adde0109"})
public class Ambassador {

  public ProxyServer server;
  public final Logger logger;
  private final Metrics.Factory metricsFactory;
  private final Path dataDirectory;

  public AmbassadorConfig config;

  private static final MapWithExpiration<String, RegisteredServer> TEMPORARY_FORCED = new MapWithExpiration<>();

  private static Ambassador instance;
  public static Ambassador getInstance() {
    return instance;
  }


  @Inject
  public Ambassador(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory, Metrics.Factory metricsFactory) {
    this.server = server;
    this.logger = logger;
    this.dataDirectory = dataDirectory;
    this.metricsFactory = metricsFactory;
    Ambassador.instance = this;
  }

  @Subscribe(order = PostOrder.LAST)
  public void onProxyInitialization(ProxyInitializeEvent event) {
    initMetrics();

    try {
      Files.createDirectories(dataDirectory);

      Path configPath = dataDirectory.resolve("Ambassador.toml");
      config = AmbassadorConfig.read(configPath);
      config.validate();

      inject();

      server.getEventManager().register(this, new VelocityEventHandler(this));
    } catch (Throwable e) {
      logger.error(e.toString());
    }
  }

  @Subscribe
  public void onProxyReload(ProxyReloadEvent event) {
    try {
      Path configPath = dataDirectory.resolve("Ambassador.toml");
      final AmbassadorConfig newconfig = AmbassadorConfig.read(configPath);
      newconfig.validate();

      config = newconfig;
    } catch (Exception e) {
      logger.error(e.toString());
      logger.warn("Reload unsuccessful, old config will be used.");
    }
  }

  private void inject() throws Throwable {
    MethodHandle cmField = MethodHandles.privateLookupIn(VelocityServer.class, MethodHandles.lookup())
            .findGetter(VelocityServer.class, "cm", ConnectionManager.class);

    ServerChannelInitializerHolder serverChannelInitializer = ((ConnectionManager) cmField.invoke(server)).getServerChannelInitializer();
    serverChannelInitializer.set(new VelocityServerChannelInitializer(serverChannelInitializer.get(),(VelocityServer) server));

    BackendChannelInitializerHolder backendChannelInitializer = ((ConnectionManager) cmField.invoke(server)).getBackendChannelInitializer();
    backendChannelInitializer.set(new VelocityBackendChannelInitializer(backendChannelInitializer.get(),(VelocityServer) server));

    Method argumentRegistry = ArgumentPropertyRegistry.class.getDeclaredMethod("register", ArgumentIdentifier.class, Class.class, ArgumentPropertySerializer.class);
    argumentRegistry.setAccessible(true);
    argumentRegistry.invoke(null,ArgumentIdentifier.id("forge:enum", mapSet(MINECRAFT_1_19, 50)), EnumArgumentProperty.class, EnumArgumentPropertySerializer.ENUM);
    argumentRegistry.invoke(null,ArgumentIdentifier.id("forge:modid", mapSet(MINECRAFT_1_19, 51)), ModIdArgumentProperty.class,
            new ArgumentPropertySerializer<>() {
              @Override
              public ModIdArgumentProperty deserialize(ByteBuf buf, ProtocolVersion protocolVersion) {
                return new ModIdArgumentProperty();
              }

              @Override
              public void serialize(Object object, ByteBuf buf, ProtocolVersion protocolVersion) {

              }
            });

  }

  public static MapWithExpiration<String, RegisteredServer> getTemporaryForced() {
    return TEMPORARY_FORCED;
  }

  private void initMetrics() {
    Metrics metrics = metricsFactory.make(this, 15655);
  }

  public static class MapWithExpiration<K, V> {

    private final Map<K, ExpiringValue<V, Long>> expirationMap = new HashMap<>();


    public V remove(K key) {
      ExpiringValue<V, Long> expiringValue = expirationMap.remove(key);
      if (expiringValue != null && expiringValue.value > System.currentTimeMillis()) {
        return expiringValue.key;
      } else {
        return null;
      }
    }

    public void put(K key, V value, int expirationTime, TimeUnit unit) {
      expirationMap.values().removeIf((v) -> v.value <= System.currentTimeMillis());
      expirationMap.put(key, new ExpiringValue<>(value,System.currentTimeMillis() + unit.toMillis(expirationTime)));
    }

    private record ExpiringValue<K, V>(K key, V value) {
    }

  }
}
