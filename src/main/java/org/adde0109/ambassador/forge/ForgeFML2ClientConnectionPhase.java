package org.adde0109.ambassador.forge;

import com.velocitypowered.proxy.connection.MinecraftConnection;
import com.velocitypowered.proxy.connection.client.ClientConnectionPhase;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import com.velocitypowered.proxy.protocol.packet.LoginPluginMessage;
import io.netty.buffer.Unpooled;
import org.adde0109.ambassador.velocity.VelocityForgeClientConnectionPhase;
import org.adde0109.ambassador.velocity.VelocityForgeHandshakeSessionHandler;

public class ForgeFML2ClientConnectionPhase implements VelocityForgeClientConnectionPhase {

  private final MinecraftConnection connection;
  ForgeFML2ClientConnectionPhase(MinecraftConnection connection) {
    this.connection = connection;
  }
  @Override
  public void handleLogin(ForgeHandshakeUtils.CachedServerHandshake handshake) {
    this.connection.setSessionHandler(new VelocityForgeHandshakeSessionHandler());
    if(handshake == null) {
      this.connection.write(new LoginPluginMessage(98,"fml:loginwrapper", Unpooled.wrappedBuffer(ForgeHandshakeUtils.generateResetPacket())));
    } else {
      this.connection.delayedWrite(new LoginPluginMessage(1,"fml:loginwrapper", Unpooled.wrappedBuffer(handshake.modListPacket)));
      for (int i = 0;i<handshake.otherPackets.size();i++) {
        this.connection.delayedWrite(new LoginPluginMessage(i+2,"fml:loginwrapper", Unpooled.wrappedBuffer(handshake.otherPackets.get(i))));
      }
      this.connection.flush();
    }
  }
}
