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
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.StateRegistry;
import com.velocitypowered.proxy.protocol.packet.LoginPluginMessage;
import com.velocitypowered.proxy.protocol.packet.LoginPluginResponse;
import com.velocitypowered.proxy.protocol.packet.PluginMessage;
import com.velocitypowered.proxy.protocol.packet.ServerLoginSuccess;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import net.kyori.adventure.text.Component;
import org.adde0109.ambassador.velocity.VelocityForgeClientConnectionPhase;
import org.adde0109.ambassador.velocity.VelocityForgeHandshakeSessionHandler;
import org.checkerframework.checker.units.qual.A;
import org.checkerframework.checker.units.qual.C;

import javax.annotation.Nullable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.*;

public class ForgeFML2ClientConnectionPhase implements VelocityForgeClientConnectionPhase {
  private boolean isResettable;

  //TODO: Use modData inside ConnectedPlayer instead
  public byte[] modListData;

  private final ArrayList<Integer> listenerList = new ArrayList();
  private Runnable whenComplete;
  public ClientPhase clientPhase = ClientPhase.HANDSHAKE;
  @Override
  public void handleLogin(ConnectedPlayer player,ForgeHandshakeUtils.CachedServerHandshake handshake, Continuation continuation) {
    final MinecraftConnection connection = player.getConnection();
    VelocityForgeHandshakeSessionHandler sessionHandler = new VelocityForgeHandshakeSessionHandler(connection.getSessionHandler(), player);
    //Without initial modlist for now
    if (true) {
      connection.delayedWrite(new LoginPluginMessage(0, "fml:loginwrapper", Unpooled.wrappedBuffer(ForgeHandshakeUtils.emptyModlist)));
      listenerList.add(0);
    } else {
      connection.delayedWrite(new LoginPluginMessage(0, "fml:loginwrapper", Unpooled.wrappedBuffer(handshake.modListPacket)));
      listenerList.add(0);
      for (int i = 0; i < handshake.otherPackets.size(); i++) {
        connection.delayedWrite(new LoginPluginMessage(i + 1, "fml:loginwrapper", Unpooled.wrappedBuffer(handshake.otherPackets.get(i))));
        listenerList.add(i + 1);
      }
    }
    this.whenComplete = () -> {
      this.clientPhase = ClientPhase.MODDED;
      continuation.resume();
    };
    connection.setSessionHandler(sessionHandler);
    connection.flush();
  }

  @Override
  public boolean handle(ConnectedPlayer player, LoginPluginResponse packet) {
    if (packet.getId() == 98) {
      this.clientPhase = ClientPhase.HANDSHAKE;
    } else if (packet.getId() == 0) {
      this.clientPhase = ClientPhase.MODLIST;
      if (modListData == null) {
        modListData = ByteBufUtil.getBytes(packet.content());
      }
    }
    if (!listenerList.removeIf(id -> id.equals(packet.getId()))) {
      player.getConnectionInFlight().getConnection().write(packet.retain());
    } else if (listenerList.isEmpty() && whenComplete != null) {
      whenComplete.run();
      whenComplete = null;
    }
    return true;
  }
  public void reset(ConnectedPlayer player, Runnable whenComplete) {
    this.whenComplete = whenComplete;
    if (player.getConnectedServer() != null) {
      player.getConnectedServer().disconnect();
    }
    MinecraftConnection connection = player.getConnection();
    connection.setSessionHandler(new VelocityForgeHandshakeSessionHandler(connection.getSessionHandler(),player));
    if (connection.getState() == StateRegistry.LOGIN) {
      connection.write(new LoginPluginMessage(98,"fml:loginwrapper", Unpooled.wrappedBuffer(ForgeHandshakeUtils.generateResetPacket())));
    } else {
      connection.write(new PluginMessage("fml:handshake",Unpooled.wrappedBuffer(ForgeHandshakeUtils.generatePluginResetPacket())));
      connection.setState(StateRegistry.LOGIN);
    }
    listenerList.add(98);
    this.clientPhase = null;
  }
  public void complete(VelocityServer server, ConnectedPlayer player, MinecraftConnection connection) {
    VelocityConfiguration configuration = (VelocityConfiguration) server.getConfiguration();
    UUID playerUniqueId = player.getUniqueId();
    if (configuration.getPlayerInfoForwardingMode() == PlayerInfoForwarding.NONE) {
      playerUniqueId = UuidUtils.generateOfflinePlayerUuid(player.getUsername());
    }
    ServerLoginSuccess success = new ServerLoginSuccess();
    success.setUsername(player.getUsername());
    success.setUuid(playerUniqueId);
    connection.write(success);

    this.clientPhase = this.clientPhase == ClientPhase.MODLIST ? ClientPhase.MODDED : ClientPhase.VANILLA;

    connection.setState(StateRegistry.PLAY);
    connection.setSessionHandler(((VelocityForgeHandshakeSessionHandler) connection.getSessionHandler()).getOriginal());
  }
  public void send(ConnectedPlayer player, MinecraftPacket message) {
      player.getConnection().write(message);
  }

  public enum ClientPhase {
    VANILLA,
    HANDSHAKE,
    MODLIST,
    MODDED
  }
}
