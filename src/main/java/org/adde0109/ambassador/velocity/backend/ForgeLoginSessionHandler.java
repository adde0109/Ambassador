package org.adde0109.ambassador.velocity.backend;

import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.config.PlayerInfoForwarding;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.connection.backend.*;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.packet.Disconnect;
import com.velocitypowered.proxy.protocol.packet.LoginPluginMessagePacket;
import com.velocitypowered.proxy.protocol.packet.ServerLoginSuccess;
import com.velocitypowered.proxy.util.except.QuietRuntimeException;
import io.netty.buffer.Unpooled;
import org.adde0109.ambassador.forge.*;

public class ForgeLoginSessionHandler implements MinecraftSessionHandler {

  private final LoginSessionHandler original;
  private final VelocityServerConnection serverConnection;
  private final VelocityServer server;

  public ForgeLoginSessionHandler(LoginSessionHandler original, VelocityServerConnection serverConnection, VelocityServer server) {
    this.original = original;
    this.serverConnection = serverConnection;
    this.server = server;
  }

  @Override
  public boolean handle(ServerLoginSuccess packet) {
    if ((serverConnection.getPhase() instanceof VelocityForgeBackendConnectionPhase phase)) {
      phase.onLoginSuccess(serverConnection,serverConnection.getPlayer());
    }

    original.handle(packet);  //Can lead to disconnect.

    //If we are still connected after handling that package.
    if (serverConnection.getConnection() != null) {
      ConnectedPlayer player = serverConnection.getPlayer();

      ((VelocityForgeClientConnectionPhase) player.getPhase()).complete(player);
    }

    return true;
  }



  @Override
  public boolean handle(Disconnect packet) {
    return original.handle(packet);
  }

  @Override
  public void disconnected() {
      original.disconnected();
  }

  public void handleGeneric(MinecraftPacket packet) {
    if (!packet.handle(original))
      original.handleGeneric(packet);
  }

  public MinecraftSessionHandler getOriginal() {
    return this.original;
  }
}
