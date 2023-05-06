package org.adde0109.ambassador.forge.packet;

import com.velocitypowered.proxy.protocol.packet.LoginPluginResponse;

public interface IForgeLoginWrapperPacket<T> {
  public T read(LoginPluginResponse message);
  public LoginPluginResponse encode();
  public int getId();
}
