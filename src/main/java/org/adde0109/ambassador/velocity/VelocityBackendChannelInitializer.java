package org.adde0109.ambassador.velocity;

import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.network.BackendChannelInitializer;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import org.adde0109.ambassador.forge.ForgeConstants;
import org.adde0109.ambassador.velocity.backend.FMLMarkerAdder;
import org.adde0109.ambassador.velocity.backend.VelocityForgeBackendHandshakeHandler;
import org.slf4j.Logger;


import java.lang.reflect.Method;

public class VelocityBackendChannelInitializer extends BackendChannelInitializer {

  private final Method INIT_CHANNEL;

  private final ChannelInitializer<Channel> delegate;
  private final VelocityServer server;

  private final Logger logger;

  public VelocityBackendChannelInitializer(ChannelInitializer<Channel> delegate, VelocityServer server, Logger logger) {
    super(server);
    this.delegate = delegate;
    this.server = server;
    this.logger = logger;
    try {
      logger.info("Respecting the previous registered BackendChannelInitializer: " + delegate.getClass().getName());
      INIT_CHANNEL = delegate.getClass().getDeclaredMethod("initChannel", Channel.class);
      INIT_CHANNEL.setAccessible(true);
    } catch (ReflectiveOperationException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  protected void initChannel(Channel ch) {
    try {
      logger.info("Calling the underlying backend channel initializer: " + delegate.getClass().getName());
      INIT_CHANNEL.invoke(delegate, ch);
    } catch (ReflectiveOperationException e) {
      throw new RuntimeException(e);
    }
    ch.pipeline().addLast(ForgeConstants.MARKER_ADDER, new FMLMarkerAdder(server));
    ch.pipeline().addLast(ForgeConstants.HANDLER, new VelocityForgeBackendHandshakeHandler(server));
  }
}
