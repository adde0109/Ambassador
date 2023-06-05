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
  private Method INIT_CHANNEL;

  private final ChannelInitializer<?> delegate;

  public VelocityServerChannelInitializer(ChannelInitializer<?> delegate,VelocityServer server) {
    super(server);
    this.delegate = delegate;
    try {
      INIT_CHANNEL = delegate.getClass().getDeclaredMethod("initChannel", Channel.class);
      INIT_CHANNEL.setAccessible(true);
    } catch (ReflectiveOperationException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  protected void initChannel(@NotNull Channel ch){
    try {
      INIT_CHANNEL.invoke(delegate,ch);
    } catch (ReflectiveOperationException e) {
     throw new RuntimeException(e);
    }
    finally {
      if (ch.pipeline().get(MinecraftConnection.class) == null)
        super.initChannel(ch);
      MinecraftConnection handler = ch.pipeline().get(MinecraftConnection.class);
      HandshakeSessionHandler originalSessionHandler = (HandshakeSessionHandler) handler.getSessionHandler();
      handler.setSessionHandler(new VelocityHandshakeSessionHandler(originalSessionHandler, handler));
    }
  }
}
