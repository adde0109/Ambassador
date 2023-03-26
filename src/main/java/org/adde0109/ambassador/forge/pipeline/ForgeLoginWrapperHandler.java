package org.adde0109.ambassador.forge.pipeline;

import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.ReferenceCountUtil;
import org.adde0109.ambassador.forge.packet.IForgeLoginWrapperPacket;
import org.adde0109.ambassador.forge.VelocityForgeClientConnectionPhase;

public class ForgeLoginWrapperHandler extends SimpleChannelInboundHandler<IForgeLoginWrapperPacket> {

  private final ConnectedPlayer player;


  public ForgeLoginWrapperHandler(ConnectedPlayer player) {
    super(false);
    this.player = player;
  }

  @Override
  protected void channelRead0(ChannelHandlerContext ctx, IForgeLoginWrapperPacket msg) throws Exception {
      VelocityForgeClientConnectionPhase phase = (VelocityForgeClientConnectionPhase) player.getPhase();
      phase.handle(player,msg,player.getConnectionInFlight());
  }
}
