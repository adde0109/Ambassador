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
import org.adde0109.ambassador.forge.ForgeFML2ClientConnectionPhase;

import java.util.concurrent.CountDownLatch;

public class VelocityForgeBackendHandshakeHandler extends ChannelDuplexHandler {

  private VelocityServerConnection serverConnection;
  private final VelocityServer server;

  public VelocityForgeBackendHandshakeHandler(VelocityServer server) {
    this.server = server;
  }

  @Override
  public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
    if ((msg instanceof ServerLogin)) {
      ChannelHandler handler = ctx.pipeline().get(Connections.HANDLER);
      if (handler instanceof MinecraftConnection connection) {
        if (connection.getAssociation() instanceof VelocityServerConnection serverConnection) {
          if (serverConnection.getPlayer().getPhase() instanceof ForgeFML2ClientConnectionPhase phase) {
            init(connection,serverConnection);
            if (phase.clientPhase == ForgeFML2ClientConnectionPhase.ClientPhase.MODDED) {
              phase.reset(serverConnection.getPlayer(), () -> {
                ctx.write(msg, promise);
                ctx.flush();
              });
            } else {
              ctx.write(msg,promise);
            }
          } else {
            ctx.pipeline().remove(this);
            ctx.write(msg,promise);
          }
        } else {
          throw new Exception("Connection not associated with a server connection");
        }
      } else {
        throw new Exception("Default minecraft packet handler not found");
      }
    } else {
      ctx.write(msg,promise);
    }
  }

  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
    if (msg instanceof LoginPluginMessage message && message.getChannel().equals("fml:loginwrapper")) {
      ((VelocityForgeBackendConnectionPhase) serverConnection.getPhase()).handle(serverConnection, serverConnection.getPlayer(), message);
      ReferenceCountUtil.release(msg);
    } else if (msg instanceof ServerLoginSuccess) {
      ((VelocityForgeBackendConnectionPhase) serverConnection.getPhase()).handleSuccess(serverConnection,server);
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
