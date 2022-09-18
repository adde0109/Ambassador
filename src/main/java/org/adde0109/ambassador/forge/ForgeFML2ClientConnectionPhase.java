package org.adde0109.ambassador.forge;

import com.velocitypowered.api.event.Continuation;
import com.velocitypowered.proxy.connection.MinecraftConnection;
import com.velocitypowered.proxy.connection.client.ClientConnectionPhase;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import com.velocitypowered.proxy.protocol.packet.LoginPluginMessage;
import com.velocitypowered.proxy.protocol.packet.LoginPluginResponse;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import org.adde0109.ambassador.velocity.VelocityForgeClientConnectionPhase;
import org.adde0109.ambassador.velocity.VelocityForgeHandshakeSessionHandler;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Optional;

public class ForgeFML2ClientConnectionPhase implements VelocityForgeClientConnectionPhase {
  private boolean isResettable;

  //TODO: Use modData inside ConnectedPlayer instead
  public byte[] modListData;

  private final ArrayList<Integer> listenerList = new ArrayList();
  private Continuation whenComplete;
  @Override
  public void handleLogin(ConnectedPlayer player,ForgeHandshakeUtils.CachedServerHandshake handshake, Continuation continuation) {
    this.whenComplete = continuation;
    final MinecraftConnection connection = player.getConnection();
    VelocityForgeHandshakeSessionHandler sessionHandler = new VelocityForgeHandshakeSessionHandler(connection.getSessionHandler(),player);
    if(handshake == null) {
      connection.delayedWrite(new LoginPluginMessage(98,"fml:loginwrapper", Unpooled.wrappedBuffer(ForgeHandshakeUtils.generateResetPacket())));
      listenerList.add(98);
    } else {
      connection.delayedWrite(new LoginPluginMessage(1,"fml:loginwrapper", Unpooled.wrappedBuffer(handshake.modListPacket)));
      listenerList.add(1);
      for (int i = 0;i<handshake.otherPackets.size();i++) {
        connection.delayedWrite(new LoginPluginMessage(i+2,"fml:loginwrapper", Unpooled.wrappedBuffer(handshake.otherPackets.get(i))));
        listenerList.add(i+2);
      }
    }
    connection.setSessionHandler(sessionHandler);
    connection.flush();
  }

  @Override
  public boolean handle(ConnectedPlayer player, LoginPluginResponse packet) {
    if (!listenerList.removeIf(id -> id.equals(packet.getId()))) {
      return false;
    }
    if (packet.getId() == 98) {
      isResettable = packet.isSuccess();
    } else if (packet.getId() == 1) {
      if (!packet.isSuccess()) {
        //TODO: Write disconnect message to end user
        player.getConnection().close();
        return true;
      }
      modListData = ByteBufUtil.getBytes(packet.content());
    }
    if (listenerList.isEmpty()) {
      whenComplete.resume();
    }
    return true;
  }
}
