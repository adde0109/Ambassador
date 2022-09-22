package org.adde0109.ambassador.velocity;

import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.connection.MinecraftConnection;
import com.velocitypowered.proxy.connection.backend.VelocityServerConnection;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import org.adde0109.ambassador.forge.ForgeConstants;
import org.adde0109.ambassador.forge.ForgeFML2ClientConnectionPhase;
import org.adde0109.ambassador.forge.ForgeFML2ConnectionType;
import org.adde0109.ambassador.velocity.backend.VelocityForgeBackendHandshakeHandler;
import org.jetbrains.annotations.NotNull;

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
  protected void initChannel(@NotNull Channel ch) throws Exception {
    INIT_CHANNEL.invoke(original);
  }

  @Override
  public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
    if (ctx.handler() instanceof MinecraftConnection connection) {
      if (connection.getAssociation() instanceof VelocityServerConnection serverConnection) {
         if (serverConnection.getPlayer().getPhase() instanceof ForgeFML2ClientConnectionPhase) {
           connection.setType(new ForgeFML2ConnectionType());
           ctx.pipeline().addBefore(ctx.name(), ForgeConstants.HANDLER, new VelocityForgeBackendHandshakeHandler(connection, serverConnection, server));
         }
      }
    }
    super.handlerAdded(ctx);
  }
}
