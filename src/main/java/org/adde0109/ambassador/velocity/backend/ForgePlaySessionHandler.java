package org.adde0109.ambassador.velocity.backend;

import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.connection.backend.TransitionSessionHandler;
import com.velocitypowered.proxy.connection.backend.VelocityServerConnection;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.packet.JoinGame;
import org.adde0109.ambassador.velocity.VelocityForgeClientConnectionPhase;

public class ForgePlaySessionHandler implements MinecraftSessionHandler {

  private final TransitionSessionHandler original;
  private final VelocityServerConnection serverConnection;

  public ForgePlaySessionHandler(TransitionSessionHandler original, VelocityServerConnection serverConnection) {
    this.original = original;
    this.serverConnection = serverConnection;
  }

  @Override
  public boolean handle(JoinGame packet) {
    if (serverConnection.getPlayer().getPhase() instanceof VelocityForgeClientConnectionPhase clientPhase) {
      serverConnection.getPlayer().setPhase(VelocityForgeClientConnectionPhase.COMPLETE);
    }
    return MinecraftSessionHandler.super.handle(packet);
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
