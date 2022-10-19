package org.adde0109.ambassador.forge;

import com.velocitypowered.api.event.Continuation;
import com.velocitypowered.api.event.player.KickedFromServerEvent;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.util.UuidUtils;
import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.config.PlayerInfoForwarding;
import com.velocitypowered.proxy.config.VelocityConfiguration;
import com.velocitypowered.proxy.connection.MinecraftConnection;
import com.velocitypowered.proxy.connection.backend.VelocityServerConnection;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import com.velocitypowered.proxy.network.Connections;
import com.velocitypowered.proxy.protocol.StateRegistry;
import com.velocitypowered.proxy.protocol.packet.LoginPluginMessage;
import com.velocitypowered.proxy.protocol.packet.LoginPluginResponse;
import com.velocitypowered.proxy.protocol.packet.PluginMessage;
import com.velocitypowered.proxy.protocol.packet.ServerLoginSuccess;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.text.Component;
import org.adde0109.ambassador.velocity.VelocityForgeClientConnectionPhase;
import org.adde0109.ambassador.velocity.VelocityForgeHandshakeSessionHandler;
import org.adde0109.ambassador.velocity.VelocityLoginPayloadManager;

import java.util.ArrayList;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class FML2CRPMClientConnectionPhase extends VelocityForgeClientConnectionPhase {
  private static String OUTBOUND_CATCHER_NAME = "ambassador-catcher";

  //TODO: Use modData inside ConnectedPlayer instead
  public byte[] modListData;
  private RegisteredServer backupServer;
  @Override
  public void handleLogin(ConnectedPlayer player, VelocityServer server, Continuation continuation) {
    final MinecraftConnection connection = player.getConnection();
    VelocityForgeHandshakeSessionHandler sessionHandler = new VelocityForgeHandshakeSessionHandler(connection.getSessionHandler(), player);
    getPayloadManager().sendPayload("fml:loginwrapper",Unpooled.wrappedBuffer(ForgeHandshakeUtils.emptyModlist)).thenAccept((data) -> {
      if (modListData == null)
        modListData = ByteBufUtil.getBytes(data);
      this.clientPhase = ClientPhase.MODDED;
      continuation.resume();
    });
    connection.setSessionHandler(sessionHandler);
    connection.flush();
  }

  public void reset(VelocityServerConnection serverConnection, ConnectedPlayer player, Runnable whenComplete) {
    if (player.getConnectedServer() != null) {
      backupServer = player.getConnectedServer().getServer();
      player.getConnectedServer().disconnect();
      player.setConnectedServer(null);
    }

    MinecraftConnection connection = player.getConnection();
    connection.setSessionHandler(new VelocityForgeHandshakeSessionHandler(connection.getSessionHandler(),player));


    if (connection.getState() == StateRegistry.LOGIN) {
      getPayloadManager().sendPayload("fml:loginwrapper", Unpooled.wrappedBuffer(ForgeHandshakeUtils.generateResetPacket()));
    } else {
      connection.write(new PluginMessage("fml:handshake",Unpooled.wrappedBuffer(ForgeHandshakeUtils.generatePluginResetPacket())));
      connection.setState(StateRegistry.LOGIN);
    }
    getPayloadManager().listenFor(98).thenAccept((ignored) -> {
      this.clientPhase = ClientPhase.HANDSHAKE;
      whenComplete.run();
    });

    this.clientPhase = null;
    connection.getChannel().pipeline().addBefore(Connections.HANDLER,OUTBOUND_CATCHER_NAME,new FML2CRPMOutgoingCatcher());
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
    try {
      connection.getChannel().pipeline().remove(OUTBOUND_CATCHER_NAME);
    } catch (NoSuchElementException ignored) {
    }
  }

  public void handleKick(KickedFromServerEvent event) {
    if (backupServer != null && !(event.getResult() instanceof KickedFromServerEvent.RedirectPlayer)) {
      net.kyori.adventure.text.Component reason = event.getServerKickReason().orElse(null);
      event.setResult(KickedFromServerEvent.RedirectPlayer.create(backupServer,reason));
      backupServer = null;
    }
  }

}
