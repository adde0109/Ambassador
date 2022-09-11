package org.adde0109.ambassador.velocity;

import com.velocitypowered.api.event.Continuation;
import com.velocitypowered.proxy.connection.MinecraftConnection;
import com.velocitypowered.proxy.connection.client.ClientConnectionPhase;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;

public class ForgeClientConnectionPhase implements ClientConnectionPhase {

  ForgeClientConnectionPhase(MinecraftConnection connection) {
  }
  public void handleLogin() {

  }
}
