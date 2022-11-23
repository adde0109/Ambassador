package org.adde0109.ambassador.velocity;

import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import com.velocitypowered.proxy.protocol.packet.LoginPluginResponse;
import io.netty.buffer.ByteBuf;

public class VelocityForgeHandshakeSessionHandler implements MinecraftSessionHandler {
  private final MinecraftSessionHandler original;
  private final VelocityForgeClientConnectionPhase phase;

  public VelocityForgeHandshakeSessionHandler(MinecraftSessionHandler original, VelocityForgeClientConnectionPhase phase) {
    this.original = original;
    this.phase = phase;
  }

  @Override
  public boolean handle(LoginPluginResponse packet) {
    if (phase.getPayloadManager().handlePayload(packet)) {
      return true;
    } else {
      return original.handle(packet);
    }
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