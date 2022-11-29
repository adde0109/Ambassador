package org.adde0109.ambassador.velocity.backend;

import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.connection.backend.LoginSessionHandler;
import com.velocitypowered.proxy.connection.backend.VelocityServerConnection;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.packet.Disconnect;
import com.velocitypowered.proxy.protocol.packet.LoginPluginMessage;
import com.velocitypowered.proxy.protocol.packet.ServerLoginSuccess;
import io.netty.buffer.ByteBuf;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.adde0109.ambassador.forge.ForgeConstants;
import org.adde0109.ambassador.forge.ForgeFMLConnectionType;
import org.adde0109.ambassador.velocity.VelocityForgeClientConnectionPhase;

public class ForgeHandshakeSessionHandler implements MinecraftSessionHandler {

  private final LoginSessionHandler original;
  private final VelocityServerConnection serverConnection;
  private final VelocityServer server;

  public ForgeHandshakeSessionHandler(LoginSessionHandler original, VelocityServerConnection serverConnection, VelocityServer server) {
    this.original = original;
    this.serverConnection = serverConnection;
    this.server = server;
  }

  @Override
  public boolean handle(LoginPluginMessage packet) {
    if (packet.getChannel().equals("fml:loginwrapper")) {
      if (!(serverConnection.getConnection().getType() instanceof ForgeFMLConnectionType)) {
        if (!(serverConnection.getPlayer().getConnection().getType() instanceof ForgeFMLConnectionType clientType)) {
          final String reason = "This server has mods that require Forge to be installed on the client. Contact your server admin for more details.";
          original.handle(Disconnect.create(Component.text(reason, NamedTextColor.RED),serverConnection.getPlayer().getProtocolVersion()));
          return true;
        }
        serverConnection.getConnection().setType(clientType);
        serverConnection.setConnectionPhase(clientType.getInitialBackendPhase());
      }
      ((VelocityForgeBackendConnectionPhase) serverConnection.getPhase()).handle(serverConnection,serverConnection.getPlayer(),packet);
      return true;
    }
    return original.handle(packet);
  }

  @Override
  public boolean handle(ServerLoginSuccess packet) {
    if ((serverConnection.getPlayer().getPhase() instanceof VelocityForgeClientConnectionPhase phase)) {
      phase.complete(server,serverConnection.getPlayer(),serverConnection.getPlayer().getConnection());
    }
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
