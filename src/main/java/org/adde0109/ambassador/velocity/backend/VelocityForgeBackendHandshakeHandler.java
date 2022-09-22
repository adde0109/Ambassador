package org.adde0109.ambassador.velocity.backend;

import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.connection.MinecraftConnection;
import com.velocitypowered.proxy.connection.backend.VelocityServerConnection;
import com.velocitypowered.proxy.protocol.packet.LoginPluginMessage;
import com.velocitypowered.proxy.protocol.packet.ServerLoginSuccess;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;

public class VelocityForgeBackendHandshakeHandler extends ChannelInboundHandlerAdapter {

  private final MinecraftConnection connection;
  private final VelocityServerConnection serverConnection;
  private final VelocityServer server;

  public VelocityForgeBackendHandshakeHandler(MinecraftConnection connection, VelocityServerConnection serverConnection, VelocityServer server) {
    this.connection = connection;
    this.serverConnection = serverConnection;
    this.server = server;
  }

  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) {
    if (msg instanceof LoginPluginMessage message && message.getChannel().equals("fml:loginwrapper")) {
      ((VelocityForgeBackendConnectionPhase) serverConnection.getPhase()).handle(serverConnection, serverConnection.getPlayer(), message);
      ReferenceCountUtil.release(msg);
    } else if (msg instanceof ServerLoginSuccess) {
      ((VelocityForgeBackendConnectionPhase) serverConnection.getPhase()).handleSuccess(serverConnection,server);
      ReferenceCountUtil.release(msg);
    } else {
      ctx.fireChannelRead(msg);
    }
  }
}
