package org.adde0109.ambassador.velocity;

import com.velocitypowered.api.event.Continuation;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.permission.PermissionsSetupEvent;
import com.velocitypowered.api.event.player.ServerLoginPluginMessageEvent;
import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.connection.ConnectionTypes;
import com.velocitypowered.proxy.connection.MinecraftConnection;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.connection.backend.BackendConnectionPhases;
import com.velocitypowered.proxy.connection.backend.VelocityServerConnection;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import com.velocitypowered.proxy.protocol.packet.LoginPluginMessage;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import org.adde0109.ambassador.Ambassador;
import org.adde0109.ambassador.forge.ForgeFML2ClientConnectionPhase;
import org.adde0109.ambassador.forge.ForgeFML2ConnectionType;
import org.adde0109.ambassador.velocity.backend.VelocityForgeBackendConnectionPhase;
import org.adde0109.ambassador.velocity.backend.VelocityForgeBackendHandshakeSessionHandler;

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
    ambassador.forgeHandshakeHandler.handleLogin(player,continuation);
  }

  @Subscribe
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
      connection.setType(new ForgeFML2ConnectionType());
      serverCon.setConnectionPhase(new VelocityForgeBackendConnectionPhase(ambassador));
      byte[] response = ((VelocityForgeBackendConnectionPhase)serverCon.getPhase()).generateResponse(serverCon.getPlayer(), Unpooled.wrappedBuffer(event.getContents()));
      event.setResult(ServerLoginPluginMessageEvent.ResponseResult.reply(response));
      continuation.resume();
      MinecraftSessionHandler sessionHandler = new VelocityForgeBackendHandshakeSessionHandler(connection.getSessionHandler(),serverCon);
      connection.setSessionHandler(sessionHandler);
    });
  }
}
