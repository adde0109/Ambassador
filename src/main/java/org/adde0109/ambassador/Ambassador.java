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

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.connection.MinecraftConnection;
import com.velocitypowered.proxy.network.ConnectionManager;
import com.velocitypowered.proxy.protocol.StateRegistry;
import com.velocitypowered.proxy.protocol.packet.DisconnectPacket;
import com.velocitypowered.proxy.protocol.packet.brigadier.ArgumentIdentifier;
import com.velocitypowered.proxy.protocol.packet.brigadier.ArgumentPropertyRegistry;
import com.velocitypowered.proxy.protocol.packet.brigadier.ArgumentPropertySerializer;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
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

@Plugin(id = "ambassador", name = "Ambassador", version = "1.5.1-beta", authors = {"adde0109"})
public class Ambassador {

  //Don't forget to update checkCompatibleVersion() when changing this value
  private static final String minVelocityVersion = "velocity-3.3.0-SNAPSHOT-330";

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

  boolean checkCompatibleVersion() {
    //Update this when changing minVelocityVersion
    try {
      Class.forName("com.velocitypowered.proxy.protocol.packet.DisconnectPacket");
    } catch (ClassNotFoundException e) {
        throw new RuntimeException(e);
    }
      return true;
  }

  @Subscribe(order = PostOrder.LAST)
  public void onProxyInitialization(ProxyInitializeEvent event) {
    initMetrics();

    try {
      if (!checkCompatibleVersion()) {
        logger.error("Incompatible velocity version! Please use '" + minVelocityVersion + "' or higher.");
        return;
      }

      Files.createDirectories(dataDirectory);

      Path configPath = dataDirectory.resolve("Ambassador.toml");
      config = AmbassadorConfig.read(configPath);

      inject();

      server.getEventManager().register(this, new VelocityEventHandler(this));
    } catch (Exception e) {
      logger.error("An error prevented Ambassador to load correctly: "+ e.toString());
    }
  }

  @Subscribe
  public void onProxyReload(ProxyReloadEvent event) {
    try {
      Path configPath = dataDirectory.resolve("Ambassador.toml");
      final AmbassadorConfig newconfig = AmbassadorConfig.read(configPath);

      config = newconfig;
    } catch (Exception e) {
      logger.error(e.toString());
      logger.warn("Reload unsuccessful, old config will be used.");
    }
  }

  private void inject() throws ReflectiveOperationException {
    Field cmField = VelocityServer.class.getDeclaredField("cm");
    cmField.setAccessible(true);

    ConnectionManager connectionManager = (ConnectionManager) cmField.get(server);

    ChannelInitializer<Channel> original = connectionManager.getServerChannelInitializer().get();
    connectionManager.getServerChannelInitializer().set(new VelocityServerChannelInitializer(original, (VelocityServer) server));

    ChannelInitializer<Channel> originalBackend = connectionManager.getBackendChannelInitializer().get();
    connectionManager.getBackendChannelInitializer().set(new VelocityBackendChannelInitializer(originalBackend, (VelocityServer) server));

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
