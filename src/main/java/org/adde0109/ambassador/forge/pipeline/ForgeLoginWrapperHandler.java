package org.adde0109.ambassador.forge.pipeline;

import com.velocitypowered.proxy.connection.MinecraftConnectionAssociation;
import com.velocitypowered.proxy.connection.backend.VelocityServerConnection;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.ReferenceCountUtil;
import org.adde0109.ambassador.forge.ForgeConstants;
import org.adde0109.ambassador.forge.ForgeFMLConnectionType;
import org.adde0109.ambassador.forge.VelocityForgeBackendConnectionPhase;
import org.adde0109.ambassador.forge.packet.Context;
import org.adde0109.ambassador.forge.packet.IForgeLoginWrapperPacket;
import org.adde0109.ambassador.forge.VelocityForgeClientConnectionPhase;

public class ForgeLoginWrapperHandler extends SimpleChannelInboundHandler<IForgeLoginWrapperPacket<?>> {

  private final MinecraftConnectionAssociation connection;


  public ForgeLoginWrapperHandler(MinecraftConnectionAssociation connection) {
    super(false);
    this.connection = connection;
  }

  @Override
  protected void channelRead0(ChannelHandlerContext ctx, IForgeLoginWrapperPacket msg) throws Exception {
    if (connection instanceof ConnectedPlayer player) {
      VelocityForgeClientConnectionPhase phase = (VelocityForgeClientConnectionPhase) player.getPhase();
      phase.handle(player,msg,player.getConnectionInFlight());
    } else if (connection instanceof VelocityServerConnection serverConnection){
      if (!(serverConnection.getConnection().getType() instanceof ForgeFMLConnectionType)) {
        serverConnection.getConnection().setType(serverConnection.getPlayer().getConnection().getType());
        serverConnection.setConnectionPhase(serverConnection.getConnection().getType().getInitialBackendPhase());
      }

      VelocityForgeBackendConnectionPhase phase =
              (VelocityForgeBackendConnectionPhase) serverConnection.getPhase();
      phase.handle(serverConnection, serverConnection.getPlayer(), msg);
    }
  }
}
