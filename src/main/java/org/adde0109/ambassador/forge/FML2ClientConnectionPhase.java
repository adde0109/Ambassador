package org.adde0109.ambassador.forge;

import com.velocitypowered.api.event.Continuation;
import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.connection.MinecraftConnection;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import com.velocitypowered.proxy.connection.client.LoginSessionHandler;
import com.velocitypowered.proxy.protocol.packet.LoginPluginResponse;
import org.adde0109.ambassador.velocity.VelocityForgeClientConnectionPhase;
import org.adde0109.ambassador.velocity.VelocityLoginPayloadManager;

import java.lang.reflect.Method;

public class FML2ClientConnectionPhase implements VelocityForgeClientConnectionPhase {


  @Override
  public void handleLogin(ConnectedPlayer player, VelocityServer server, Continuation continuation) {
    MinecraftSessionHandler sessionHandler = player.getConnection().getSessionHandler();
    if (player.getConnection().getSessionHandler() == null) {
      continuation.resumeWithException(new Exception("No current player session handler"));
      return;
    }
    if (!(player.getConnection().getSessionHandler() instanceof LoginSessionHandler)) {
      continuation.resumeWithException(new Exception("Invalid current player session handler:" + player.getConnection().getSessionHandler().getClass().getName()));
      return;
    }
    try {
      Method connectToInitalServer = LoginSessionHandler.class.getDeclaredMethod("connectToInitialServer");
      connectToInitalServer.setAccessible(true);
      connectToInitalServer.invoke(sessionHandler);
    } catch (ReflectiveOperationException e) {
      continuation.resumeWithException(e);
    }
  }

  @Override
  public boolean handle(ConnectedPlayer player, LoginPluginResponse packet) {
    return VelocityForgeClientConnectionPhase.super.handle(player, packet);
  }
}
