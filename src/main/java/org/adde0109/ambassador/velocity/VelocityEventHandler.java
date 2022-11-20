package org.adde0109.ambassador.velocity;

import com.velocitypowered.api.event.Continuation;
import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.event.permission.PermissionsSetupEvent;
import com.velocitypowered.api.event.player.KickedFromServerEvent;
import com.velocitypowered.api.event.player.PlayerChooseInitialServerEvent;
import com.velocitypowered.api.event.player.ServerPostConnectEvent;
import com.velocitypowered.api.event.player.ServerPreConnectEvent;
import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.connection.backend.VelocityServerConnection;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import com.velocitypowered.proxy.protocol.packet.ClientSettings;
import org.adde0109.ambassador.Ambassador;
import org.adde0109.ambassador.forge.FML2CRPMClientConnectionPhase;
import org.adde0109.ambassador.forge.FML2ClientConnectionPhase;

public class VelocityEventHandler {

  private final Ambassador ambassador;

  public VelocityEventHandler(Ambassador ambassador) {
    this.ambassador = ambassador;
  }

  @Subscribe
  public void onPermissionsSetupEvent(PermissionsSetupEvent event, Continuation continuation) {
    if(!(event.getSubject() instanceof ConnectedPlayer player)) {
      continuation.resume();
      return;
    }
    if (!(player.getPhase() instanceof VelocityForgeClientConnectionPhase phase)) {
      continuation.resume();
      return;
    }
    player.getConnection().eventLoop().submit(() -> phase.fireLoginEvent(player, (VelocityServer) ambassador.server,continuation));
  }

  @Subscribe(order = PostOrder.LAST)
  public void onKickedFromServerEvent(KickedFromServerEvent event, Continuation continuation) {
    if (((ConnectedPlayer) event.getPlayer()).getPhase() instanceof FML2CRPMClientConnectionPhase phase) {
      phase.handleKick(event);
    }
    continuation.resume();
  }

  @Subscribe(order = PostOrder.LAST)
  public void onServerPreConnectEvent(ServerPreConnectEvent event, Continuation continuation) {
    ConnectedPlayer player = (ConnectedPlayer) event.getPlayer();
    if (!(player.getPhase() instanceof VelocityForgeClientConnectionPhase phase)) {
      continuation.resume();
      return;
    }
    if (phase.internalServerConnection != null) {
      event.setResult(ServerPreConnectEvent.ServerResult.denied());
      player.setConnectedServer((VelocityServerConnection) phase.internalServerConnection);
      phase.internalServerConnection = null;
    }
    continuation.resume();
  }

  @Subscribe(order = PostOrder.LAST)
  public void onPlayerChooseInitialServerEvent(PlayerChooseInitialServerEvent event, Continuation continuation) {
    ConnectedPlayer player = (ConnectedPlayer) event.getPlayer();
    if (!(player.getPhase() instanceof VelocityForgeClientConnectionPhase phase)) {
      continuation.resume();
      return;
    }
    if (event.getInitialServer().isEmpty())
      event.setInitialServer(phase.internalServerConnection.getServer());
    continuation.resume();
  }

  @Subscribe(order = PostOrder.LAST)
  public void onServerPostConnectEvent(ServerPostConnectEvent event, Continuation continuation) {
    ConnectedPlayer player = (ConnectedPlayer) event.getPlayer();
    if (!(player.getPhase() instanceof VelocityForgeClientConnectionPhase phase)) {
      continuation.resume();
      return;
    }
    if (phase instanceof FML2ClientConnectionPhase specialPhase) {
      specialPhase.handleJoinGame();
    }
    //if (((ConnectedPlayer) event.getPlayer()).getConnectedServer() != null && ((ConnectedPlayer) event.getPlayer()).getConnectedServer().getConnection() != null) {
    //  ((ConnectedPlayer) event.getPlayer()).getConnectedServer().getConnection().write(new ClientSettings("en_GB", (byte) 10, 0, true, (short) 0xFF,1,false,true));
    //}
    continuation.resume();
  }

  @Subscribe
  public void onLoginEvent(PostLoginEvent event, Continuation continuation) {
    ConnectedPlayer player = (ConnectedPlayer) event.getPlayer();
    if (!(player.getPhase() instanceof VelocityForgeClientConnectionPhase phase)) {
      continuation.resume();
      return;
    }

    if (phase instanceof FML2ClientConnectionPhase specialPhase) {
      specialPhase.awaitJoinGame().thenAcceptAsync((ignored) -> {
        player.setConnectedServer(null);
        continuation.resume();
      },player.getConnection().eventLoop());
    }
  }
}
