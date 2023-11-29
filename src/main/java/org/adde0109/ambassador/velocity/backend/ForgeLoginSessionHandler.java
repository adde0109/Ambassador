package org.adde0109.ambassador.velocity.backend;

import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.config.PlayerInfoForwarding;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.connection.backend.*;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.packet.Disconnect;
import com.velocitypowered.proxy.protocol.packet.LoginPluginMessage;
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
    original.handle(packet);
    if (serverConnection.getConnection() == null) {
      return true;
    }
    ConnectedPlayer player = serverConnection.getPlayer();
    if (!(serverConnection.getConnection().getType() instanceof ForgeFMLConnectionType)) {
      if (player.getConnectedServer() == null ||
              player.getConnectedServer().getConnection().getType() instanceof ForgeFMLConnectionType) {
        //Initial Vanilla - test if the client can be reset
        //Forge -> vanilla
        player.getPhase().resetConnectionPhase(player);
        player.getConnectionInFlight().getConnection().getChannel().config().setAutoRead(false);
      }
    } else {
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
    //Same as default just not safe.
    if (!serverConnection.getPlayer().getPhase().consideredComplete()) {
      if (server.getConfiguration().getPlayerInfoForwardingMode() == PlayerInfoForwarding.LEGACY) {
        serverConnection.getPlayer().handleConnectionException(serverConnection.getServer(),
                new QuietRuntimeException("The connection to the remote server was unexpectedly closed.\n"
                        + "This is usually because the remote server does not have BungeeCord IP forwarding "
                        + "correctly enabled.\nSee https://velocitypowered.com/wiki/users/forwarding/ "
                        + "for instructions on how to configure player info forwarding correctly."),
        false);
      } else {
        serverConnection.getPlayer().handleConnectionException(serverConnection.getServer(),
                new QuietRuntimeException("The connection to the remote server was unexpectedly closed."),
        false);
      }
      return;
    }
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
