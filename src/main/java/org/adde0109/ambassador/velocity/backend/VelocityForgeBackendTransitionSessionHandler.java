package org.adde0109.ambassador.velocity.backend;

import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.connection.backend.TransitionSessionHandler;
import com.velocitypowered.proxy.connection.backend.VelocityServerConnection;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import com.velocitypowered.proxy.protocol.StateRegistry;
import com.velocitypowered.proxy.protocol.packet.LoginPluginMessage;
import com.velocitypowered.proxy.protocol.packet.PluginMessage;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public class VelocityForgeBackendTransitionSessionHandler implements MinecraftSessionHandler {
  private final TransitionSessionHandler original;
  private final VelocityServerConnection serverCon;

  public VelocityForgeBackendTransitionSessionHandler(MinecraftSessionHandler original, VelocityServerConnection serverCon) {
    this.original = (TransitionSessionHandler) original;
    this.serverCon = serverCon;
  }

  @Override
  public boolean beforeHandle() {
    return original.beforeHandle();
  }

  @Override
  public void disconnected() {
    original.disconnected();
  }

  @Override
  public void handleGeneric(MinecraftPacket packet) {
    packet.handle(original);
  }

  @Override
  public boolean handle(PluginMessage packet) {
    if (serverCon.getPlayer().getConnection().getState() == StateRegistry.LOGIN) {
      //So it can't send when the client is in LOGIN state.
      //We can instead use forge's LoginWrapper
      ByteBuf wrapped = Unpooled.buffer();
      ProtocolUtils.writeString(wrapped,"minecraft:register");
      ProtocolUtils.writeVarInt(wrapped, packet.content().readableBytes());
      wrapped.writeBytes(packet.content());
      serverCon.getPlayer().getConnection().write(new LoginPluginMessage(97,"fml:loginwrapper",wrapped));
    } else {
      original.handle(packet);
    }
    return true;
  }

}
