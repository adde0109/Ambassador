package org.adde0109.ambassador.forge;

import com.velocitypowered.api.util.GameProfile;
import com.velocitypowered.proxy.config.PlayerInfoForwarding;
import com.velocitypowered.proxy.connection.ConnectionType;
import com.velocitypowered.proxy.connection.MinecraftConnection;
import com.velocitypowered.proxy.connection.backend.BackendConnectionPhase;
import com.velocitypowered.proxy.connection.backend.BackendConnectionPhases;
import com.velocitypowered.proxy.connection.client.ClientConnectionPhase;
import org.adde0109.ambassador.velocity.backend.VelocityForgeBackendConnectionPhase;

public class ForgeFML2ConnectionType implements ConnectionType {

  @Override
  public ClientConnectionPhase getInitialClientPhase() {
    return new ForgeFML2ClientConnectionPhase();
  }

  @Override
  public BackendConnectionPhase getInitialBackendPhase() {
    return BackendConnectionPhases.UNKNOWN;
  }

  @Override
  public GameProfile addGameProfileTokensIfRequired(GameProfile original, PlayerInfoForwarding forwardingType) {
    return original;
  }
}
