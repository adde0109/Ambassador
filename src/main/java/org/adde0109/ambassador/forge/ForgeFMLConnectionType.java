package org.adde0109.ambassador.forge;

import com.velocitypowered.api.util.GameProfile;
import com.velocitypowered.proxy.config.PlayerInfoForwarding;
import com.velocitypowered.proxy.connection.ConnectionType;
import com.velocitypowered.proxy.connection.backend.BackendConnectionPhase;
import com.velocitypowered.proxy.connection.client.ClientConnectionPhase;
import org.adde0109.ambassador.velocity.backend.VelocityForgeBackendConnectionPhase;

import java.util.Collections;

public class ForgeFMLConnectionType implements ConnectionType {

  final int netVersion;

  public ForgeFMLConnectionType(int netVersion) {
    this.netVersion = netVersion;
  }

  @Override
  public ClientConnectionPhase getInitialClientPhase() {
    return new FML2ClientConnectionPhase();
  }

  @Override
  public BackendConnectionPhase getInitialBackendPhase() {
    return new VelocityForgeBackendConnectionPhase();
  }

  @Override
  public GameProfile addGameProfileTokensIfRequired(GameProfile original, PlayerInfoForwarding forwardingType) {
    //This is meant for Arc light to parse
    if (forwardingType == PlayerInfoForwarding.LEGACY) {
      return original.addProperties(Collections.singleton(new GameProfile.Property("extraData", "\1FML" + netVersion + "\1", "")));
    } else {
      return original;
    }
  }
}
