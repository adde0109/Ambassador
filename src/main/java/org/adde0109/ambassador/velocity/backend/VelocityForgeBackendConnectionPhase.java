package org.adde0109.ambassador.velocity.backend;

import com.google.common.io.ByteArrayDataInput;
import com.velocitypowered.proxy.connection.MinecraftConnection;
import com.velocitypowered.proxy.connection.backend.BackendConnectionPhase;
import com.velocitypowered.proxy.connection.backend.VelocityServerConnection;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import com.velocitypowered.proxy.protocol.packet.LoginPluginMessage;
import com.velocitypowered.proxy.protocol.packet.LoginPluginResponse;
import com.velocitypowered.proxy.protocol.packet.PluginMessage;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.adde0109.ambassador.forge.ForgeFML2ClientConnectionPhase;
import org.adde0109.ambassador.forge.ForgeHandshakeUtils;
import org.adde0109.ambassador.velocity.VelocityForgeClientConnectionPhase;

import java.io.EOFException;

public class VelocityForgeBackendConnectionPhase implements BackendConnectionPhase {

  public boolean handle(VelocityServerConnection server, ConnectedPlayer player, LoginPluginMessage message) {
    if (!message.getChannel().equals("fml:loginwrapper") || !(player.getPhase() instanceof VelocityForgeClientConnectionPhase)) {
      return false;
    }
    MinecraftConnection connection = server.getConnection();
    if (connection == null) {
      throw new NullPointerException();
    }

    ByteBuf content = message.content();
    content.readBytes(14);  //Channel Identifier
    ProtocolUtils.readVarInt(content); //Length
    int packetID = ProtocolUtils.readVarInt(content);


    switch (packetID) {

      case 1:
        final byte[] data = ((ForgeFML2ClientConnectionPhase) player.getPhase()).modListData;
        connection.write(new LoginPluginResponse(message.getId(),true, Unpooled.wrappedBuffer(data)));
        break;

      default:
        connection.write(new LoginPluginResponse(message.getId(),true,Unpooled.wrappedBuffer(ForgeHandshakeUtils.ACKPacket)));
        break;
    }

    return true;
  }
}
