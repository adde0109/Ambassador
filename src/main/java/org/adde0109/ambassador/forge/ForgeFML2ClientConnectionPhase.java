package org.adde0109.ambassador.forge;

import com.velocitypowered.api.event.Continuation;
import com.velocitypowered.api.proxy.Player;
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
import org.checkerframework.checker.units.qual.A;
import org.checkerframework.checker.units.qual.C;

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
  public ArrayList<LoginPluginMessage> packagesToSendAfterReset = new ArrayList<>();
  private Runnable whenComplete;
  public boolean isReady = false;
  @Override
  public void handleLogin(ConnectedPlayer player,ForgeHandshakeUtils.CachedServerHandshake handshake, Continuation continuation) {
    this.whenComplete = continuation::resume;
    final MinecraftConnection connection = player.getConnection();
    VelocityForgeHandshakeSessionHandler sessionHandler = new VelocityForgeHandshakeSessionHandler(connection.getSessionHandler(),player);
    if(handshake == null) {
      connection.delayedWrite(new LoginPluginMessage(0,"fml:loginwrapper", Unpooled.wrappedBuffer(ForgeHandshakeUtils.generateEmptyModlist())));
      listenerList.add(0);
    } else {
      connection.delayedWrite(new LoginPluginMessage(0,"fml:loginwrapper", Unpooled.wrappedBuffer(handshake.modListPacket)));
      listenerList.add(0);
      for (int i = 0;i<handshake.otherPackets.size();i++) {
        connection.delayedWrite(new LoginPluginMessage(i+1,"fml:loginwrapper", Unpooled.wrappedBuffer(handshake.otherPackets.get(i))));
        listenerList.add(i+1);
      }
    }
    connection.setSessionHandler(sessionHandler);
    connection.flush();
  }

  @Override
  public boolean handle(ConnectedPlayer player, LoginPluginResponse packet) {
    if (!listenerList.removeIf(id -> id.equals(packet.getId()))) {
      player.getConnectionInFlight().getConnection().write(packet.retain());
      return true;
    }
    if (packet.getId() == 98) {
      for (LoginPluginMessage packet1 : packagesToSendAfterReset) {
        player.getConnection().delayedWrite(packet1);
      }
      packagesToSendAfterReset = new ArrayList<>();
      player.getConnection().flush();
    } else if (packet.getId() == 0) {
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
  public void reset(ConnectedPlayer player,MinecraftConnection connection, byte[] serverModlist) {
    isReady = false;
    if (player.getConnectedServer() != null) {
      player.getConnectedServer().disconnect();
    }
    connection.setSessionHandler(new VelocityForgeHandshakeSessionHandler(connection.getSessionHandler(),player));
    connection.write(new PluginMessage("fml:handshake",Unpooled.wrappedBuffer(ForgeHandshakeUtils.generatePluginResetPacket())));
    listenerList.add(98);
    send(player,new LoginPluginMessage(0,"fml:loginwrapper",Unpooled.wrappedBuffer(serverModlist)));
    listenerList.add(0);
    connection.setState(StateRegistry.LOGIN);
  }
  public void send(ConnectedPlayer player, LoginPluginMessage message) {
    if (isReady) {
      player.getConnection().write(message);
    } else {
      packagesToSendAfterReset.add(message);
    }
  }
}
