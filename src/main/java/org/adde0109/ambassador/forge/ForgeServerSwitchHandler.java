package org.adde0109.ambassador.forge;

import com.velocitypowered.api.event.Continuation;
import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.event.player.ServerPreConnectEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.messages.ChannelIdentifier;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import com.velocitypowered.api.util.GameProfile;

import com.velocitypowered.api.util.UuidUtils;
import com.velocitypowered.proxy.config.PlayerInfoForwarding;
import com.velocitypowered.proxy.config.VelocityConfiguration;
import com.velocitypowered.proxy.connection.MinecraftConnection;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import com.velocitypowered.proxy.protocol.StateRegistry;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.*;

import com.velocitypowered.proxy.protocol.packet.LoginPluginMessage;
import com.velocitypowered.proxy.protocol.packet.LoginPluginResponse;
import com.velocitypowered.proxy.protocol.packet.PluginMessage;
import com.velocitypowered.proxy.protocol.packet.ServerLoginSuccess;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.EventLoop;
import io.netty.util.concurrent.GenericFutureListener;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.adde0109.ambassador.Ambassador;

import org.adde0109.ambassador.AmbassadorConfig;
import org.apache.commons.collections4.map.PassiveExpiringMap;

public class ForgeServerSwitchHandler {
  private final Ambassador ambassador;
  public final PassiveExpiringMap<String,ForgeServerConnection> reSyncMap;

  public ForgeServerSwitchHandler(Ambassador ambassador) {
    this.ambassador = ambassador;
    this.reSyncMap = new PassiveExpiringMap<>(ambassador.config.getReSyncTimeout(),TimeUnit.SECONDS);
  }


  @Subscribe(order = PostOrder.LAST)
  public void onServerPreConnectEvent(ServerPreConnectEvent event, Continuation continuation) {
    if (!event.getResult().isAllowed()) {
      continuation.resume();
      return;
    }
    Optional<ForgeServerConnection> forgeServerConnectionOptional = ambassador.forgeHandshakeHandler.getForgeServerConnection(event.getOriginalServer());
    Optional<ForgeConnection> forgeConnection = ambassador.forgeHandshakeHandler.getForgeConnection(event.getPlayer());
    if (forgeConnection.isPresent()) {
      //TODO: If the client can resync without kick, we don't need to check the server.
      if (true) {
        continuation.resume();
        return;
      }
      ForgeServerConnection forgeServerConnection = forgeServerConnectionOptional.orElseGet(() -> new ForgeServerConnection(event.getOriginalServer()));
      forgeServerConnection.getHandshake().whenComplete((msg, ex) -> {
        if (ex != null) {
          //The server was forge but aren't right now. Or it's just offline.
          if (ex instanceof ForgeHandshakeUtils.HandshakeReceiver.HandshakeNotAvailableException) {
            //It's not running ambassador, so it should be unregistered.
            if (forgeServerConnectionOptional.isPresent())
              ambassador.forgeHandshakeHandler.unRegisterForgeServer(forgeServerConnection.getServer());
          }
        } else {
          //If the server just got discovered, register it.
          if (forgeServerConnectionOptional.isEmpty())
            ambassador.forgeHandshakeHandler.registerForgeServer(event.getOriginalServer(),forgeServerConnection);

          //To make legacy forwarding work
          List<GameProfile.Property> properties = new ArrayList<>(event.getPlayer().getGameProfileProperties());
          properties.add(new GameProfile.Property("extraData", "\1FML2\1",""));
          event.getPlayer().setGameProfileProperties(properties);

          if (ambassador.config.reSyncOptionForge() != AmbassadorConfig.reSyncOption.NEVER) {
            if (forgeConnection.get().getTransmittedHandshake().isEmpty() || !msg.equals(forgeConnection.get().getTransmittedHandshake().get())) {
              event.setResult(ServerPreConnectEvent.ServerResult.denied());
              kickReSync(event.getPlayer(), forgeServerConnection);
            }
          }
        }
        continuation.resume();
      });
    } else if (forgeServerConnectionOptional.isPresent()) {
      //If vanilla tries to connect to a server we know is forge
      event.setResult(ServerPreConnectEvent.ServerResult.denied());
      event.getPlayer().sendMessage(Component.text("This server requires Forge!", NamedTextColor.RED));
      continuation.resume();
    } else {
      //The server is not known to us.
      continuation.resume();
    }
  }
  private void kickReSync(Player player, ForgeServerConnection forgeServerConnection){
    ambassador.logger.info("Kicking {} because of re-sync needed", player);
    player.disconnect(Component.text("Please reconnect"));
    reSyncMap.put(player.getUsername(),forgeServerConnection);
  }

  @Subscribe
  public void onServerConnectedEvent(ServerConnectedEvent event, Continuation continuation) {
    ConnectedPlayer player = ((ConnectedPlayer) event.getPlayer());
    Optional<ForgeServerConnection> forgeServerConnection = ambassador.forgeHandshakeHandler.getForgeServerConnection(event.getServer());
    Optional<ForgeConnection> forgeConnection = ambassador.forgeHandshakeHandler.getForgeConnection(player);
    if (forgeConnection.isPresent() && forgeServerConnection.isPresent() && event.getPreviousServer().isPresent()) {
      Future<ForgeHandshakeUtils.CachedServerHandshake> handshakeFuture = forgeServerConnection.get().getHandshake();
        player.getConnection().eventLoop().submit(() -> {
          reSync(player,handshakeFuture,continuation);
        });
    } else {
      continuation.resume();
    }
  }

  private void reSync(ConnectedPlayer player, Future<ForgeHandshakeUtils.CachedServerHandshake> handshakeFuture, Continuation continuation) {
    MinecraftConnection connection = player.getConnection();
    connection.setSessionHandler(new ReSyncHandler(player,handshakeFuture,continuation));
    connection.write(new PluginMessage("fml:handshake", Unpooled.wrappedBuffer(ForgeHandshakeUtils.generateResetPacket())));
    connection.setState(StateRegistry.LOGIN);
  }
  private class ReSyncHandler implements MinecraftSessionHandler {


    private final Player player;

    private final MinecraftConnection connection;
    private final Future<ForgeHandshakeUtils.CachedServerHandshake> handshakeFuture;
    private final MinecraftSessionHandler originalHandler;
    private final Continuation continuation;
    private int sent = 0;

    ReSyncHandler(ConnectedPlayer player, Future<ForgeHandshakeUtils.CachedServerHandshake> handshakeFuture, Continuation continuation) {
      this.player = player;
      this.connection = player.getConnection();
      this.handshakeFuture = handshakeFuture;
      this.originalHandler = this.connection.getSessionHandler();
      this.continuation = continuation;
    }

    @Override
    public boolean handle(LoginPluginResponse packet) {
      if (sent == 0) {
        ForgeHandshakeUtils.CachedServerHandshake handshake;
        try {
          handshake = handshakeFuture.get();
        } catch (Exception e) {
          return true;
        }
        sent = sendHandshake(connection, handshake);
      } else {
        if (sent == 1) {
          complete();
        }
        sent--;
      }
      return true;
    }

    private int sendHandshake(MinecraftConnection connection, ForgeHandshakeUtils.CachedServerHandshake handshake) {
      int transactionId = 1;
      connection.delayedWrite(new LoginPluginMessage(transactionId, "fml:loginwrapper", Unpooled.wrappedBuffer(handshake.modListPacket)));
      for (byte[] data : handshake.otherPackets) {
        transactionId++;
        connection.delayedWrite(new LoginPluginMessage(transactionId, "fml:loginwrapper", Unpooled.wrappedBuffer(data)));
      }
      connection.flush();
      return transactionId;
    }
    private void complete() {
      VelocityConfiguration configuration = (VelocityConfiguration) ambassador.server.getConfiguration();
      UUID playerUniqueId = player.getUniqueId();
      if (configuration.getPlayerInfoForwardingMode() == PlayerInfoForwarding.NONE) {
        playerUniqueId = UuidUtils.generateOfflinePlayerUuid(player.getUsername());
      }
      ServerLoginSuccess success = new ServerLoginSuccess();
      success.setUsername(player.getUsername());
      success.setUuid(playerUniqueId);
      connection.write(success);
      connection.setState(StateRegistry.PLAY);
      connection.setSessionHandler(originalHandler);
      continuation.resume();
    }
  }
}
