package org.adde0109.ambassador.velocity.backend;

import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.config.PlayerInfoForwarding;
import com.velocitypowered.proxy.connection.MinecraftConnection;
import com.velocitypowered.proxy.connection.backend.LoginSessionHandler;
import com.velocitypowered.proxy.connection.backend.VelocityServerConnection;
import com.velocitypowered.proxy.network.Connections;
import com.velocitypowered.proxy.protocol.packet.Handshake;
import com.velocitypowered.proxy.protocol.packet.LoginPluginMessage;
import com.velocitypowered.proxy.protocol.packet.ServerLoginSuccess;
import io.netty.channel.*;
import io.netty.util.ReferenceCountUtil;
import org.adde0109.ambassador.forge.ForgeConstants;
import org.adde0109.ambassador.forge.ForgeFMLConnectionType;
import org.adde0109.ambassador.velocity.VelocityForgeClientConnectionPhase;
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

    if (serverConnection.getPlayer().getConnection().getType() instanceof ForgeFMLConnectionType) {
      connection.setSessionHandler(new ForgeHandshakeSessionHandler((LoginSessionHandler) connection.getSessionHandler(),serverConnection,server));
    }

    ctx.pipeline().remove(this);
    ctx.pipeline().fireChannelActive();
  }
}
