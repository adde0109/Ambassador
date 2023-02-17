package org.adde0109.ambassador.velocity;

import com.velocitypowered.api.event.Continuation;
import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.event.player.*;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import com.velocitypowered.proxy.protocol.StateRegistry;
import org.adde0109.ambassador.Ambassador;

public class VelocityEventHandler {

  private final Ambassador ambassador;

  public VelocityEventHandler(Ambassador ambassador) {
    this.ambassador = ambassador;
  }

  @Subscribe
  public void onLoginEvent(LoginEvent event, Continuation continuation) {
    ConnectedPlayer player = (ConnectedPlayer) event.getPlayer();
    if (player.getPhase() instanceof VelocityForgeClientConnectionPhase) {
      player.getConnection().eventLoop().submit(() -> player.getConnection().setState(StateRegistry.LOGIN));
    }
    continuation.resume();
  }

  @Subscribe
  public void onPostLoginEvent(PostLoginEvent event, Continuation continuation) {
    ConnectedPlayer player = (ConnectedPlayer) event.getPlayer();
    if (player.getPhase() instanceof VelocityForgeClientConnectionPhase phase) {
      VelocityForgeHandshakeSessionHandler sessionHandler = new VelocityForgeHandshakeSessionHandler(player.getConnection().getSessionHandler(), player);
      player.getConnection().eventLoop().submit(() -> player.getConnection().setSessionHandler(sessionHandler));
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
    RegisteredServer chosenServer = Ambassador.getTemporaryForced().remove(player.getUsername());
    if (chosenServer != null)
      event.setInitialServer(chosenServer);
    continuation.resume();
  }

}
