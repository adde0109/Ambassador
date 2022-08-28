package org.adde0109.ambassador.forge;

import com.velocitypowered.proxy.connection.MinecraftConnection;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.packet.LoginPluginMessage;
import com.velocitypowered.proxy.protocol.packet.LoginPluginResponse;
import io.netty.buffer.Unpooled;

public class ForgeSessionHandler implements MinecraftSessionHandler {
  private final MinecraftSessionHandler delegate;
  private final MinecraftConnection connection;

  public ForgeSessionHandler(MinecraftSessionHandler delegate, MinecraftConnection connection) {
    this.delegate = delegate;
    this.connection = connection;
  }

  @Override
  public void handleGeneric(MinecraftPacket packet) {
    packet.handle(delegate);
  }

  @Override
  public void activated() {
  }

  @Override
  public boolean handle(LoginPluginResponse packet) {
    //DETECT FML or PCF
    return false;
}
