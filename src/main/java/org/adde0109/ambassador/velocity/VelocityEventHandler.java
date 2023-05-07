package org.adde0109.ambassador.velocity;

import com.velocitypowered.api.event.Continuation;
import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.event.player.*;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import com.velocitypowered.proxy.network.Connections;
import com.velocitypowered.proxy.protocol.StateRegistry;
import net.kyori.adventure.text.Component;
import org.adde0109.ambassador.Ambassador;
import org.adde0109.ambassador.forge.ForgeConstants;
import org.adde0109.ambassador.forge.VelocityForgeClientConnectionPhase;
import org.adde0109.ambassador.forge.pipeline.ForgeLoginWrapperDecoder;
import org.adde0109.ambassador.forge.pipeline.ForgeLoginWrapperHandler;

public class VelocityEventHandler {

  private final Ambassador ambassador;

  public VelocityEventHandler(Ambassador ambassador) {
    this.ambassador = ambassador;
  }

  @Subscribe
  public void onLoginEvent(LoginEvent event, Continuation continuation) {
    ConnectedPlayer player = (ConnectedPlayer) event.getPlayer();
    if (player.getPhase() instanceof VelocityForgeClientConnectionPhase) {
      player.getConnection().eventLoop().submit(() -> {
        player.getConnection().setState(StateRegistry.LOGIN);

        player.getConnection().getChannel().pipeline().addBefore(
                Connections.HANDLER,
                ForgeConstants.FORGE_HANDSHAKE_DECODER, new ForgeLoginWrapperDecoder());
        player.getConnection().getChannel().pipeline().addAfter(
                ForgeConstants.FORGE_HANDSHAKE_DECODER,
                ForgeConstants.FORGE_HANDSHAKE_HANDLER, new ForgeLoginWrapperHandler(player));
      });
    }
    //event.getPlayer().sendMessage(Component.text("login event"));
    continuation.resume();
  }

  @Subscribe(order = PostOrder.FIRST)
  public void onPostLoginEvent(PostLoginEvent event, Continuation continuation) {
    if (((ConnectedPlayer) event.getPlayer()).getPhase() instanceof VelocityForgeClientConnectionPhase)
      ((VelocityServer) Ambassador.getInstance().server).unregisterConnection((ConnectedPlayer) event.getPlayer());
    //event.getPlayer().sendMessage(Component.text("post login event"));
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
    //event.getPlayer().sendMessage(Component.text("choose server event"));
    continuation.resume();
  }

}
