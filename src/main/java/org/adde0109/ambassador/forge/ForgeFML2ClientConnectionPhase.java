package org.adde0109.ambassador.forge;

import com.velocitypowered.api.event.Continuation;
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
  private Continuation whenComplete;
  ForgeFML2ClientConnectionPhase(MinecraftConnection connection) {
    this.connection = connection;
  }
  @Override
  public void handleLogin(ForgeHandshakeUtils.CachedServerHandshake handshake, Continuation continuation) {
    VelocityForgeHandshakeSessionHandler sessionHandler = new VelocityForgeHandshakeSessionHandler(this);
    if(handshake == null) {
      this.connection.write(new LoginPluginMessage(98,"fml:loginwrapper", Unpooled.wrappedBuffer(ForgeHandshakeUtils.generateResetPacket())));
      sessionHandler.listen(98);
    } else {
      this.connection.delayedWrite(new LoginPluginMessage(1,"fml:loginwrapper", Unpooled.wrappedBuffer(handshake.modListPacket)));
      sessionHandler.listen(1);
      for (int i = 0;i<handshake.otherPackets.size();i++) {
        this.connection.delayedWrite(new LoginPluginMessage(i+2,"fml:loginwrapper", Unpooled.wrappedBuffer(handshake.otherPackets.get(i))));
        sessionHandler.listen(i+2);
      }
      this.connection.setSessionHandler(sessionHandler);
      this.connection.flush();
    }
  }

  @Override
  public void handle(LoginPluginResponse packet, boolean lastMessage) {
    if (packet.getId() == 98) {
      isResettable = packet.isSuccess();
    } else {
      modListData = packet.content().retain();
    }
    if (lastMessage) {
      whenComplete.resume();
    }
  }
}
