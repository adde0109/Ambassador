package org.adde0109.ambassador.forge;

import com.velocitypowered.api.util.GameProfile;
import com.velocitypowered.proxy.config.PlayerInfoForwarding;
import com.velocitypowered.proxy.connection.ConnectionType;
import com.velocitypowered.proxy.connection.backend.BackendConnectionPhase;
import com.velocitypowered.proxy.connection.client.ClientConnectionPhase;

public class ForgeConnectionType implements ConnectionType {
  @Override
  public ClientConnectionPhase getInitialClientPhase() {
    return new ForgeClientConnectionPhase();
  }

  @Override
  public BackendConnectionPhase getInitialBackendPhase() {
    return null;
  }

  @Override
  public GameProfile addGameProfileTokensIfRequired(GameProfile original, PlayerInfoForwarding forwardingType) {
    return original;
  }
}
