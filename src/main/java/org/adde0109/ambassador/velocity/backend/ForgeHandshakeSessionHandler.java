package org.adde0109.ambassador.velocity.backend;

import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.connection.backend.LoginSessionHandler;
import com.velocitypowered.proxy.connection.backend.VelocityServerConnection;
import com.velocitypowered.proxy.protocol.packet.LoginPluginMessage;
import com.velocitypowered.proxy.protocol.packet.ServerLoginSuccess;
import io.netty.buffer.ByteBuf;

public class ForgeHandshakeSessionHandler implements MinecraftSessionHandler {

  private final LoginSessionHandler original;
  private final VelocityServerConnection serverConnection;
  private final VelocityForgeBackendConnectionPhase phase;
  private final VelocityServer server;

  public ForgeHandshakeSessionHandler(LoginSessionHandler original, VelocityServerConnection serverConnection, VelocityServer server) {
    this.original = original;
    this.serverConnection = serverConnection;
    this.phase = (VelocityForgeBackendConnectionPhase) serverConnection.getPhase();
    this.server = server;
  }

  @Override
  public boolean handle(LoginPluginMessage packet) {
    if (phase.handle(serverConnection,serverConnection.getPlayer(),packet)) {
      return true;
    } else {
      return original.handle(packet);
    }
  }

  @Override
  public boolean handle(ServerLoginSuccess packet) {
    phase.handleSuccess(serverConnection,server);
    return original.handle(packet);
  }

  @Override
  public void handleUnknown(ByteBuf buf) {
    original.handleUnknown(buf);
  }

  @Override
  public void disconnected() {
    original.disconnected();
  }

  public MinecraftSessionHandler getOriginal() {
    return this.original;
  }
}
