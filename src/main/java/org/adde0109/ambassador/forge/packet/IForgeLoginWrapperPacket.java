package org.adde0109.ambassador.forge.packet;

import com.velocitypowered.proxy.protocol.packet.LoginPluginResponse;

public interface IForgeLoginWrapperPacket {
  public LoginPluginResponse encode();
  public int getId();

  public boolean getSuccess();
}
