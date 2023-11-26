package org.adde0109.ambassador.velocity.backend;

import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.connection.MinecraftConnection;
import com.velocitypowered.proxy.connection.backend.LoginSessionHandler;
import com.velocitypowered.proxy.connection.backend.VelocityServerConnection;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import com.velocitypowered.proxy.network.Connections;
import com.velocitypowered.proxy.protocol.StateRegistry;
import io.netty.channel.*;
import org.adde0109.ambassador.forge.ForgeConstants;
import org.adde0109.ambassador.forge.ForgeFMLConnectionType;
import org.adde0109.ambassador.forge.pipeline.ForgeLoginWrapperCodec;
import org.adde0109.ambassador.forge.pipeline.ForgeLoginWrapperHandler;
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

    ConnectedPlayer player = serverConnection.getPlayer();
    if (player.getConnection().getType() instanceof ForgeFMLConnectionType) {
      ForgeLoginSessionHandler forgeLoginSessionHandler = new ForgeLoginSessionHandler((LoginSessionHandler) connection.getActiveSessionHandler(), serverConnection,server);
      connection.setActiveSessionHandler(StateRegistry.LOGIN, forgeLoginSessionHandler);

      player.getConnection().getChannel().pipeline().addBefore(
              Connections.HANDLER,
              ForgeConstants.FORGE_HANDSHAKE_DECODER, new ForgeLoginWrapperCodec());
      player.getConnection().getChannel().pipeline().addAfter(
              ForgeConstants.FORGE_HANDSHAKE_DECODER,
              ForgeConstants.FORGE_HANDSHAKE_HANDLER, new ForgeLoginWrapperHandler(player));
    }

    ctx.pipeline().fireChannelActive();
  }
}
