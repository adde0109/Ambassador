package org.adde0109.ambassador.forge;

import com.velocitypowered.proxy.connection.MinecraftConnection;
import com.velocitypowered.proxy.connection.client.ClientConnectionPhase;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import com.velocitypowered.proxy.protocol.packet.LoginPluginMessage;
import com.velocitypowered.proxy.protocol.packet.LoginPluginResponse;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.adde0109.ambassador.velocity.VelocityForgeClientConnectionPhase;
import org.adde0109.ambassador.velocity.VelocityForgeHandshakeSessionHandler;

import javax.annotation.Nullable;

public class ForgeFML2ClientConnectionPhase implements VelocityForgeClientConnectionPhase {

  private final MinecraftConnection connection;
  private boolean isResettable;
  private ByteBuf modListData;
  ForgeFML2ClientConnectionPhase(MinecraftConnection connection) {
    this.connection = connection;
  }
  @Override
  public void handleLogin(@Nullable ForgeHandshakeUtils.CachedServerHandshake handshake) {
    if(handshake == null) {
      this.connection.write(new LoginPluginMessage(98,"fml:loginwrapper", Unpooled.wrappedBuffer(ForgeHandshakeUtils.generateResetPacket())));
    } else {
      this.connection.delayedWrite(new LoginPluginMessage(1,"fml:loginwrapper", Unpooled.wrappedBuffer(handshake.modListPacket)));
      for (int i = 0;i<handshake.otherPackets.size();i++) {
        this.connection.delayedWrite(new LoginPluginMessage(i+2,"fml:loginwrapper", Unpooled.wrappedBuffer(handshake.otherPackets.get(i))));
      }
      //TODO:Register sent packets
      this.connection.setSessionHandler(new VelocityForgeHandshakeSessionHandler(this));
      this.connection.flush();
    }
  }

  @Override
  public void handle(LoginPluginResponse packet) {
    if (packet.getId() == 98) {
      isResettable = packet.isSuccess();
    } else {
      modListData = packet.content().retain();
    }
  }
}
