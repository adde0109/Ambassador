package org.adde0109.ambassador.velocity;

import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.protocol.packet.LoginPluginResponse;

import java.util.ArrayList;
import java.util.List;

public class VelocityForgeHandshakeSessionHandler implements MinecraftSessionHandler {

  private final ArrayList<Integer> listenerList = new ArrayList();
  private final VelocityForgeClientConnectionPhase phase;
  public VelocityForgeHandshakeSessionHandler(VelocityForgeClientConnectionPhase phase) {
    this.phase = phase;
  }

  @Override
  public boolean handle(LoginPluginResponse packet) {
    if (listenerList.removeIf(id -> id.equals(packet.getId()))) {
      phase.handle(packet, listenerList.isEmpty());
      return true;
    }
    return false;
  }
  public void listen(int id) {
    listenerList.add(id);
  }
}
