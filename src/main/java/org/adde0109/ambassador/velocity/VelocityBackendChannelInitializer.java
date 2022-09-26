package org.adde0109.ambassador.velocity;

import com.velocitypowered.proxy.VelocityServer;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import org.adde0109.ambassador.forge.ForgeConstants;
import org.adde0109.ambassador.velocity.backend.VelocityForgeBackendHandshakeHandler;


import java.lang.reflect.Method;

public class VelocityBackendChannelInitializer extends ChannelInitializer<Channel> {

  private static final Method INIT_CHANNEL;

  private final ChannelInitializer<?> original;
  private final VelocityServer server;

  static {
    try {
      INIT_CHANNEL = ChannelInitializer.class.getDeclaredMethod("initChannel", Channel.class);
      INIT_CHANNEL.setAccessible(true);
    } catch (ReflectiveOperationException e) {
      throw new RuntimeException(e);
    }
  }

  public VelocityBackendChannelInitializer(ChannelInitializer<?> original, VelocityServer server) {
    this.original = original;
    this.server = server;
  }

  @Override
  protected void initChannel(Channel ch) throws Exception {
    INIT_CHANNEL.invoke(original,ch);
    ch.pipeline().addLast(ForgeConstants.HANDLER, new VelocityForgeBackendHandshakeHandler(server));
  }
}
