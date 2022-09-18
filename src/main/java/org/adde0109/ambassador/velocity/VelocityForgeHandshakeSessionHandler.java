package org.adde0109.ambassador.velocity;

import com.velocitypowered.proxy.connection.MinecraftConnection;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import com.velocitypowered.proxy.connection.client.LoginSessionHandler;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.packet.LoginPluginResponse;
import io.netty.buffer.ByteBuf;
import org.adde0109.ambassador.forge.ForgeFML2ClientConnectionPhase;

import java.util.ArrayList;
import java.util.List;

public class VelocityForgeHandshakeSessionHandler implements MinecraftSessionHandler {
  private final MinecraftSessionHandler original;
  private final ConnectedPlayer player;

  public VelocityForgeHandshakeSessionHandler(MinecraftSessionHandler original, ConnectedPlayer player) {
    this.original = original;
    this.player = player;
  }

  @Override
  public boolean handle(LoginPluginResponse packet) {
    if (((VelocityForgeClientConnectionPhase) player.getPhase()).handle(player, packet)) {
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