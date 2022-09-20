package org.adde0109.ambassador.velocity.backend;

import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.connection.MinecraftConnection;
import com.velocitypowered.proxy.connection.backend.BackendConnectionPhase;
import com.velocitypowered.proxy.connection.backend.VelocityServerConnection;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import com.velocitypowered.proxy.protocol.packet.LoginPluginMessage;
import com.velocitypowered.proxy.protocol.packet.LoginPluginResponse;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.adde0109.ambassador.Ambassador;
import org.adde0109.ambassador.forge.ForgeFML2ClientConnectionPhase;
import org.adde0109.ambassador.forge.ForgeHandshakeUtils;
import org.adde0109.ambassador.velocity.VelocityForgeClientConnectionPhase;

import java.util.ArrayList;
import java.util.List;

public class VelocityForgeBackendConnectionPhase implements BackendConnectionPhase {

  private final Ambassador ambassador;
  private final List<byte[]> handshakeMessages = new ArrayList<>();

  public VelocityForgeBackendConnectionPhase(Ambassador ambassador) {
    this.ambassador = ambassador;
  }

  public void handleSuccess(VelocityServerConnection serverCon) {
    ForgeHandshakeUtils.complete((VelocityServer) ambassador.server,serverCon.getPlayer(),serverCon.getPlayer().getConnection());
    //((ForgeFML2ClientConnectionPhase) serverCon.getPlayer().getPhase()).reset(serverCon.getPlayer(),serverCon.getPlayer().getConnection(), handshakeMessages,
    //        () -> ForgeHandshakeUtils.complete((VelocityServer) ambassador.server,serverCon.getPlayer(),serverCon.getPlayer().getConnection()));
  }

  public byte[] generateResponse(ConnectedPlayer player, ByteBuf content) {
    content.readBytes(14);  //Channel Identifier
    ProtocolUtils.readVarInt(content); //Length
    int packetID = ProtocolUtils.readVarInt(content);


    switch (packetID) {

      case 1:
        final byte[] data = ((ForgeFML2ClientConnectionPhase) player.getPhase()).modListData;
        return data;

      default:
        return ForgeHandshakeUtils.ACKPacket;
    }
  }

  public boolean handle(VelocityServerConnection server, ConnectedPlayer player, LoginPluginMessage message) {
    if (!message.getChannel().equals("fml:loginwrapper") || !(player.getPhase() instanceof VelocityForgeClientConnectionPhase)) {
      return false;
    }
    message.retain();
    ((ForgeFML2ClientConnectionPhase) player.getPhase()).send(player,message);
    return true;
  }

}
