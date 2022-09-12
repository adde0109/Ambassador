package org.adde0109.ambassador.velocity;

import com.velocitypowered.proxy.connection.client.ClientConnectionPhase;
import org.adde0109.ambassador.forge.ForgeHandshakeUtils;

public interface VelocityForgeClientConnectionPhase extends ClientConnectionPhase {
  default void handleLogin(ForgeHandshakeUtils.CachedServerHandshake handshake) {

  }
}
