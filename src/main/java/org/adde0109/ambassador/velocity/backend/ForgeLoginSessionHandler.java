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
  public boolean handle(LoginPluginMessage packet) {
    if (packet.getChannel().equals("fml:loginwrapper")) {
      if (serverConnection.getPhase() == BackendConnectionPhases.UNKNOWN) {
        VelocityForgeBackendConnectionPhase.NOT_STARTED.handle(serverConnection,serverConnection.getPlayer(),packet);
      } else if (serverConnection.getPhase() instanceof VelocityForgeBackendConnectionPhase phase1) {
        phase1.handle(serverConnection,serverConnection.getPlayer(),packet);
      }
      return true;
    }
    return original.handle(packet);
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
      if (player.getConnectedServer() == null) {
        //Initial Vanilla
        //Send empty mod list in order to get client mod list
        ((VelocityForgeClientConnectionPhase) player.getPhase()).sendVanillaModlist(player);
        player.getConnectionInFlight().getConnection().getChannel().config().setAutoRead(false);
        //((VelocityForgeClientConnectionPhase) player.getPhase()).complete(player);
      } else if (player.getConnectedServer().getConnection().getType() instanceof ForgeFMLConnectionType) {
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
    if (!serverConnection.getPlayer().getPhase().consideredComplete()) {
      serverConnection.getPlayer().handleConnectionException(serverConnection.getServer(), packet, false);
      return true;
    }
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
