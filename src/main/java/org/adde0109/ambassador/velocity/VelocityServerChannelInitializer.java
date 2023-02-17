package org.adde0109.ambassador.velocity;

import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.connection.MinecraftConnection;
import com.velocitypowered.proxy.connection.client.HandshakeSessionHandler;
import com.velocitypowered.proxy.network.Connections;
import com.velocitypowered.proxy.network.ServerChannelInitializer;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import org.adde0109.ambassador.velocity.client.VelocityHandshakeSessionHandler;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;

public class VelocityServerChannelInitializer extends ServerChannelInitializer {
  private static final Method INIT_CHANNEL;

  private final ChannelInitializer<?> delegate;

  static {
    try {
      INIT_CHANNEL = ChannelInitializer.class.getDeclaredMethod("initChannel", Channel.class);
      INIT_CHANNEL.setAccessible(true);
    } catch (ReflectiveOperationException e) {
      throw new RuntimeException(e);
    }
  }

  public VelocityServerChannelInitializer(ChannelInitializer<?> delegate,VelocityServer server) {
    super(server);
    this.delegate = delegate;
  }

  @Override
  protected void initChannel(@NotNull Channel ch){
    try {
      INIT_CHANNEL.invoke(delegate,ch);
    } catch (ReflectiveOperationException e) {
     throw new RuntimeException(e);
    }
    ChannelHandler handler = ch.pipeline().get(Connections.HANDLER);
    if (!(handler instanceof final MinecraftConnection connection)) throw new RuntimeException("plugin conflict");
    HandshakeSessionHandler originalSessionHandler = (HandshakeSessionHandler) connection.getSessionHandler();
    connection.setSessionHandler(new VelocityHandshakeSessionHandler(originalSessionHandler, connection));
  }
}
