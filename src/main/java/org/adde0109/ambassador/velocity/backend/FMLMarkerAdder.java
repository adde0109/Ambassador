package org.adde0109.ambassador.velocity.backend;

import com.velocitypowered.proxy.connection.MinecraftConnection;
import com.velocitypowered.proxy.connection.backend.VelocityServerConnection;
import com.velocitypowered.proxy.network.Connections;
import com.velocitypowered.proxy.protocol.packet.Handshake;
import io.netty.channel.*;
import io.netty.handler.codec.MessageToMessageEncoder;
import org.adde0109.ambassador.forge.ForgeConstants;
import org.adde0109.ambassador.forge.ForgeFMLConnectionType;

import java.util.ArrayList;
import java.util.List;

public class FMLMarkerAdder extends MessageToMessageEncoder<Handshake> {

  public FMLMarkerAdder() {
    super(Handshake.class);
  }

  @Override
  protected void encode(ChannelHandlerContext ctx, Handshake msg, List<Object> out) {
    MinecraftConnection connection = (MinecraftConnection) ctx.pipeline().get(Connections.HANDLER);
    VelocityServerConnection serverConnection = (VelocityServerConnection) connection.getAssociation();

    if (serverConnection.getPlayer().getConnection().getType() instanceof ForgeFMLConnectionType FMLType) {
      msg.setServerAddress(msg.getServerAddress() + (FMLType == ForgeConstants.ForgeFML3 ? ForgeConstants.FML3Marker : ForgeConstants.FML2Marker));
    }
    out.add(msg);
    ctx.pipeline().remove(this);
  }
}
