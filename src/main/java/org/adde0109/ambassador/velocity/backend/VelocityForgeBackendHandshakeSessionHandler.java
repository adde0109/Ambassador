package org.adde0109.ambassador.velocity.backend;

import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.connection.backend.VelocityServerConnection;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import com.velocitypowered.proxy.protocol.packet.LoginPluginMessage;
import com.velocitypowered.proxy.protocol.packet.ServerLoginSuccess;
import org.adde0109.ambassador.velocity.VelocityForgeClientConnectionPhase;

public class VelocityForgeBackendHandshakeSessionHandler implements MinecraftSessionHandler {
  private final MinecraftSessionHandler original;
  private final VelocityServerConnection serverCon;

  public VelocityForgeBackendHandshakeSessionHandler(MinecraftSessionHandler original, VelocityServerConnection serverCon) {
    this.original = original;
    this.serverCon = serverCon;
  }

  @Override
  public void disconnected() {
    original.disconnected();
  }

  @Override
  public void exception(Throwable throwable) {
    original.exception(throwable);
  }

  @Override
  public boolean handle(LoginPluginMessage packet) {
    if (((VelocityForgeBackendConnectionPhase) serverCon.getPhase()).handle(serverCon, serverCon.getPlayer(), packet)) {
      return true;
    } else {
      return original.handle(packet);
    }
  }

  @Override
  public boolean handle(ServerLoginSuccess packet) {
    original.handle(packet);
    ((VelocityForgeBackendConnectionPhase) serverCon.getPhase()).handleSuccess(serverCon);
    return true;
  }
}
