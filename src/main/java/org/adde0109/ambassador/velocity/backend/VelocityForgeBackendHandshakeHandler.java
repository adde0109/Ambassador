package org.adde0109.ambassador.velocity.backend;

import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.config.PlayerInfoForwarding;
import com.velocitypowered.proxy.connection.MinecraftConnection;
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

public class VelocityForgeBackendHandshakeHandler extends ChannelDuplexHandler {

  private VelocityServerConnection serverConnection;
  private final VelocityServer server;

  public VelocityForgeBackendHandshakeHandler(VelocityServer server) {
    this.server = server;
  }

  @Override
  public void flush(ChannelHandlerContext ctx) throws Exception {
    if (serverConnection == null) {
      return;
    }
    VelocityForgeClientConnectionPhase clientPhase = (VelocityForgeClientConnectionPhase) serverConnection.getPlayer().getPhase();
    if (clientPhase.clientPhase == VelocityForgeClientConnectionPhase.ClientPhase.MODDED) {
      clientPhase.reset(serverConnection ,serverConnection.getPlayer()).thenAccept(ignored -> ctx.flush());
    } else {
      ctx.flush();
    }
  }

  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
    if (msg instanceof LoginPluginMessage message && message.getChannel().equals("fml:loginwrapper")) {
      ((VelocityForgeBackendConnectionPhase) serverConnection.getPhase()).handle(serverConnection, serverConnection.getPlayer(), message);
      ReferenceCountUtil.release(msg);
    } else if (msg instanceof ServerLoginSuccess) {
      ((VelocityForgeBackendConnectionPhase) serverConnection.getPhase()).handleSuccess(serverConnection,server);
      ctx.pipeline().remove(this);
      ctx.fireChannelRead(msg);
    } else {
      ctx.fireChannelRead(msg);
    }
  }

  private void initBackend(MinecraftConnection connection, VelocityServerConnection serverConnection, ForgeFMLConnectionType type) {
    this.serverConnection = serverConnection;
    connection.setType(type);
    serverConnection.setConnectionPhase(connection.getType().getInitialBackendPhase());
  }

  @Override
  public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
    if (!(msg instanceof Handshake handshake)){
      ctx.write(msg, promise);
      return;
    }
    ChannelHandler handler = ctx.pipeline().get(Connections.HANDLER);
    if (handler instanceof MinecraftConnection connection) {
      if (connection.getAssociation() instanceof VelocityServerConnection serverConnection) {
        if (serverConnection.getPlayer().getConnection().getType() instanceof ForgeFMLConnectionType type) {
          initBackend(connection,serverConnection,type);
          if (server.getConfiguration().getPlayerInfoForwardingMode() != PlayerInfoForwarding.LEGACY) {
            if (type == ForgeConstants.ForgeFML2) {
              handshake.setServerAddress(handshake.getServerAddress() + ForgeConstants.FML2Marker);
            } else if (type == ForgeConstants.ForgeFML3) {
              handshake.setServerAddress(handshake.getServerAddress() + ForgeConstants.FML3Marker);
            }
          }
        } else {
          ctx.pipeline().remove(this);
        }
      } else {
        throw new Exception("Connection not associated with a server connection");
      }
    } else {
      throw new Exception("Default minecraft packet handler not found");
    }
    ctx.write(msg,promise);
  }
}
