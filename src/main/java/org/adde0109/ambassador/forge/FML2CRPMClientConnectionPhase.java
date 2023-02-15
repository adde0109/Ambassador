package org.adde0109.ambassador.forge;

import com.velocitypowered.api.event.player.KickedFromServerEvent;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.util.UuidUtils;
import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.config.PlayerInfoForwarding;
import com.velocitypowered.proxy.config.VelocityConfiguration;
import com.velocitypowered.proxy.connection.MinecraftConnection;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import com.velocitypowered.proxy.network.Connections;
import com.velocitypowered.proxy.protocol.StateRegistry;
import com.velocitypowered.proxy.protocol.packet.PluginMessage;
import com.velocitypowered.proxy.protocol.packet.ServerLoginSuccess;
import io.netty.buffer.Unpooled;
import org.adde0109.ambassador.Ambassador;
import org.adde0109.ambassador.velocity.VelocityForgeClientConnectionPhase;
import org.adde0109.ambassador.velocity.VelocityForgeHandshakeSessionHandler;
import org.adde0109.ambassador.velocity.VelocityLoginPayloadManager;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class FML2CRPMClientConnectionPhase extends VelocityForgeClientConnectionPhase {

  //TODO: Use modData inside ConnectedPlayer instead
  public byte[] modListData;
  private RegisteredServer backupServer;

  public FML2CRPMClientConnectionPhase(VelocityForgeClientConnectionPhase.ClientPhase clientPhase, VelocityLoginPayloadManager payloadManager) {
    super(clientPhase,payloadManager);
  }

  public CompletableFuture<Boolean> reset(RegisteredServer server, ConnectedPlayer player) {
    CompletableFuture<Boolean> future = new CompletableFuture<>();
    if (player.getConnectedServer() != null) {
      backupServer = player.getConnectedServer().getServer();
      player.getConnectedServer().disconnect();
      player.setConnectedServer(null);
    }

    MinecraftConnection connection = player.getConnection();
    connection.setSessionHandler(new VelocityForgeHandshakeSessionHandler(connection.getSessionHandler(),player));

    ((VelocityServer) Ambassador.getInstance().server).unregisterConnection(player);
    this.clientPhase = null;

    ScheduledFuture<?> scheduledFuture = connection.eventLoop().schedule(()-> {
      connection.getChannel().pipeline().remove(ForgeConstants.RESET_LISTENER);
      future.complete(false);
    }, Ambassador.getInstance().config.getResetTimeout(), TimeUnit.MILLISECONDS);
    connection.getChannel().pipeline().addBefore(Connections.MINECRAFT_DECODER, ForgeConstants.RESET_LISTENER,new FML2CRPMResetCompleteDecoder());
    getPayloadManager().listenFor(98).thenAccept(ignore -> {
      if (scheduledFuture.cancel(false)) {
        connection.getChannel().pipeline().remove(ForgeConstants.RESET_LISTENER);
        connection.setState(StateRegistry.LOGIN);
        this.clientPhase = ClientPhase.HANDSHAKE;
        future.complete(true);
      }
    });
    connection.write(new PluginMessage("fml:handshake",Unpooled.wrappedBuffer(ForgeHandshakeUtils.generatePluginResetPacket())));
    return future;
  }


  public void handleKick(KickedFromServerEvent event) {
    if (backupServer != null && !(event.getResult() instanceof KickedFromServerEvent.RedirectPlayer)) {
      net.kyori.adventure.text.Component reason = event.getServerKickReason().orElse(null);
      event.setResult(KickedFromServerEvent.RedirectPlayer.create(backupServer,reason));
      backupServer = null;
    }
  }

}
