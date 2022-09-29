package org.adde0109.ambassador.velocity;

import com.velocitypowered.api.event.Continuation;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.permission.PermissionsSetupEvent;
import com.velocitypowered.api.event.player.ServerLoginPluginMessageEvent;
import com.velocitypowered.proxy.connection.MinecraftConnection;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.connection.backend.VelocityServerConnection;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import com.velocitypowered.proxy.protocol.packet.LoginPluginMessage;
import io.netty.buffer.Unpooled;
import org.adde0109.ambassador.Ambassador;
import org.adde0109.ambassador.forge.ForgeFML2ClientConnectionPhase;
import org.adde0109.ambassador.forge.ForgeFML2ConnectionType;
import org.adde0109.ambassador.velocity.backend.VelocityForgeBackendConnectionPhase;
import org.adde0109.ambassador.velocity.backend.VelocityForgeBackendHandshakeHandler;

import java.util.Objects;

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
    phase.handleLogin(player,null,continuation);
  }

  /*@Subscribe
  public void onServerLoginPluginMessageEvent(ServerLoginPluginMessageEvent event, Continuation continuation) {
    if (!Objects.equals(event.getIdentifier().getId(), "fml:loginwrapper") || !(((ConnectedPlayer)event.getConnection().getPlayer()).getPhase() instanceof VelocityForgeClientConnectionPhase)) {
      continuation.resume();
      return;
    }
    final VelocityServerConnection serverCon = (VelocityServerConnection) event.getConnection();
    final MinecraftConnection connection = serverCon.getConnection();
    if (connection == null) {
      //This should never happen.
      continuation.resumeWithException(new NullPointerException());
      return;
    }
    connection.eventLoop().submit(() -> {
      if (event.getSequenceId() == 0) {
        connection.setType(new ForgeFML2ConnectionType());
        serverCon.setConnectionPhase(new VelocityForgeBackendConnectionPhase(ambassador));
        byte[] response = ((VelocityForgeBackendConnectionPhase)serverCon.getPhase()).generateResponse(serverCon.getPlayer(), Unpooled.wrappedBuffer(event.getContents()));
        event.setResult(ServerLoginPluginMessageEvent.ResponseResult.reply(response));
        MinecraftSessionHandler sessionHandler = new VelocityForgeBackendHandshakeHandler(connection.getSessionHandler(),serverCon);
        connection.setSessionHandler(sessionHandler);
        ((ForgeFML2ClientConnectionPhase) serverCon.getPlayer().getPhase()).reset(serverCon.getPlayer(), serverCon.getPlayer().getConnection(),event.getContents());
      } else {
        ((ForgeFML2ClientConnectionPhase) serverCon.getPlayer().getPhase()).send(serverCon.getPlayer(), new LoginPluginMessage(event.getSequenceId(),event.getIdentifier().getId(),Unpooled.wrappedBuffer(event.getContents())));
      }
      continuation.resume();
    });
  }*/
}
