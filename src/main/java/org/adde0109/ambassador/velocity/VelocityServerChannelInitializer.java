package org.adde0109.ambassador.velocity;

import com.velocitypowered.proxy.connection.MinecraftConnection;
import com.velocitypowered.proxy.connection.client.HandshakeSessionHandler;
import com.velocitypowered.proxy.network.Connections;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;

public class VelocityServerChannelInitializer extends ChannelInitializer<Channel> {
  private static final Method INIT_CHANNEL;

  private final ChannelInitializer<?> original;

  static {
    try {
      INIT_CHANNEL = ChannelInitializer.class.getDeclaredMethod("initChannel", Channel.class);
      INIT_CHANNEL.setAccessible(true);
    } catch (ReflectiveOperationException e) {
      throw new RuntimeException(e);
    }
  }

  public VelocityServerChannelInitializer(ChannelInitializer<?> original) {
    this.original = original;
  }

  @Override
  protected void initChannel(@NotNull Channel ch) throws Exception {
    INIT_CHANNEL.invoke(original,ch);
    ChannelHandler handler = ch.pipeline().get(Connections.HANDLER);
    if (!(handler instanceof final MinecraftConnection connection)) throw new Exception("plugin conflict");
    HandshakeSessionHandler originalSessionHandler = (HandshakeSessionHandler) connection.getSessionHandler();
    connection.setSessionHandler(new VelocityHandshakeSessionHandler(originalSessionHandler, connection));
  }
}
