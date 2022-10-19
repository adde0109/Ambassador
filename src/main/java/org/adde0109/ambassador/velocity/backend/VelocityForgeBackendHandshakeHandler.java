package org.adde0109.ambassador.velocity.backend;

import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.connection.MinecraftConnection;
import com.velocitypowered.proxy.connection.backend.VelocityServerConnection;
import com.velocitypowered.proxy.network.Connections;
import com.velocitypowered.proxy.protocol.packet.LoginPluginMessage;
import com.velocitypowered.proxy.protocol.packet.ServerLogin;
import com.velocitypowered.proxy.protocol.packet.ServerLoginSuccess;
import io.netty.channel.*;
import io.netty.util.ReferenceCountUtil;
import org.adde0109.ambassador.forge.ForgeConstants;
import org.adde0109.ambassador.forge.FML2CRPMClientConnectionPhase;

public class VelocityForgeBackendHandshakeHandler extends ChannelDuplexHandler {

  private VelocityServerConnection serverConnection;
  private final VelocityServer server;

  public VelocityForgeBackendHandshakeHandler(VelocityServer server) {
    this.server = server;
  }

  @Override
  public void flush(ChannelHandlerContext ctx) throws Exception {
    if (serverConnection != null){
      ctx.flush();
      return;
    }
      ChannelHandler handler = ctx.pipeline().get(Connections.HANDLER);
      if (handler instanceof MinecraftConnection connection) {
        if (connection.getAssociation() instanceof VelocityServerConnection serverConnection) {
          if (serverConnection.getPlayer().getPhase() instanceof FML2CRPMClientConnectionPhase phase) {
            init(connection,serverConnection);
            if (phase.clientPhase == FML2CRPMClientConnectionPhase.ClientPhase.MODDED) {
              phase.reset(serverConnection ,serverConnection.getPlayer(), () -> {
                ctx.flush();
              });
            } else {
              ctx.flush();
            }
          } else {
            ctx.pipeline().remove(this);
            ctx.flush();
          }
        } else {
          throw new Exception("Connection not associated with a server connection");
        }
      } else {
        throw new Exception("Default minecraft packet handler not found");
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

  private void init(MinecraftConnection connection, VelocityServerConnection serverConnection) {
    this.serverConnection = serverConnection;
    connection.setType(ForgeConstants.ForgeFML2);
    serverConnection.setConnectionPhase(connection.getType().getInitialBackendPhase());
  }
}
