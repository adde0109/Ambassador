package org.adde0109.ambassador.velocity.backend;

import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.connection.MinecraftConnection;
import com.velocitypowered.proxy.connection.backend.LoginSessionHandler;
import com.velocitypowered.proxy.connection.backend.VelocityServerConnection;
import com.velocitypowered.proxy.network.Connections;
import io.netty.channel.*;
import org.adde0109.ambassador.forge.ForgeFMLConnectionType;
import org.jetbrains.annotations.NotNull;

public class VelocityForgeBackendHandshakeHandler extends ChannelInboundHandlerAdapter {

  private final VelocityServer server;

  public VelocityForgeBackendHandshakeHandler(VelocityServer server) {
    this.server = server;
  }

  @Override
  public void channelActive(@NotNull ChannelHandlerContext ctx) throws Exception {
    MinecraftConnection connection = (MinecraftConnection) ctx.pipeline().get(Connections.HANDLER);
    VelocityServerConnection serverConnection = (VelocityServerConnection) connection.getAssociation();

    ctx.pipeline().remove(this);

    if (serverConnection.getPlayer().getConnection().getType() instanceof ForgeFMLConnectionType) {
      connection.setSessionHandler(new ForgeLoginSessionHandler((LoginSessionHandler) connection.getSessionHandler(),serverConnection,server));
    }

    ctx.pipeline().fireChannelActive();
  }
}
