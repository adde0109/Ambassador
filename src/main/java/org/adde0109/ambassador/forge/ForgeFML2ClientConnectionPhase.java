package org.adde0109.ambassador.forge;

import com.velocitypowered.api.event.Continuation;
import com.velocitypowered.api.util.UuidUtils;
import com.velocitypowered.proxy.Velocity;
import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.config.PlayerInfoForwarding;
import com.velocitypowered.proxy.config.VelocityConfiguration;
import com.velocitypowered.proxy.connection.MinecraftConnection;
import com.velocitypowered.proxy.connection.client.ClientConnectionPhase;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import com.velocitypowered.proxy.protocol.StateRegistry;
import com.velocitypowered.proxy.protocol.packet.LoginPluginMessage;
import com.velocitypowered.proxy.protocol.packet.LoginPluginResponse;
import com.velocitypowered.proxy.protocol.packet.PluginMessage;
import com.velocitypowered.proxy.protocol.packet.ServerLoginSuccess;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import org.adde0109.ambassador.velocity.VelocityForgeClientConnectionPhase;
import org.adde0109.ambassador.velocity.VelocityForgeHandshakeSessionHandler;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

public class ForgeFML2ClientConnectionPhase implements VelocityForgeClientConnectionPhase {
  private boolean isResettable;

  //TODO: Use modData inside ConnectedPlayer instead
  public byte[] modListData;

  private final ArrayList<Integer> listenerList = new ArrayList();
  private Runnable whenComplete;
  @Override
  public void handleLogin(ConnectedPlayer player,ForgeHandshakeUtils.CachedServerHandshake handshake, Continuation continuation) {
    this.whenComplete = continuation::resume;
    final MinecraftConnection connection = player.getConnection();
    VelocityForgeHandshakeSessionHandler sessionHandler = new VelocityForgeHandshakeSessionHandler(connection.getSessionHandler(),player);
    if(handshake == null) {
      connection.delayedWrite(new LoginPluginMessage(1,"fml:loginwrapper", Unpooled.wrappedBuffer(ForgeHandshakeUtils.generateEmptyModlist())));
      listenerList.add(1);
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
    if (listenerList.isEmpty() && whenComplete != null) {
      whenComplete.run();
      whenComplete = null;
    }
    return true;
  }
  public void reset(ConnectedPlayer player,MinecraftConnection connection, List<byte[]> messages, Runnable whenComplete) {
    this.whenComplete = whenComplete;
    connection.setSessionHandler(new VelocityForgeHandshakeSessionHandler(connection.getSessionHandler(),player));
    connection.write(new PluginMessage("fml:handshake",Unpooled.wrappedBuffer(ForgeHandshakeUtils.generatePluginResetPacket())));
    listenerList.add(98);
    connection.setState(StateRegistry.LOGIN);
    for (int i = 0;i<messages.size();i++) {
      connection.delayedWrite(new LoginPluginMessage(i+2,"fml:loginwrapper", Unpooled.wrappedBuffer(messages.get(i))));
      listenerList.add(i+2);
    }
    connection.flush();
  }
}
