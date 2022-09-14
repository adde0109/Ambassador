package org.adde0109.ambassador.velocity;

import com.velocitypowered.proxy.connection.MinecraftConnection;
import com.velocitypowered.proxy.connection.client.ClientConnectionPhase;
import com.velocitypowered.proxy.protocol.packet.LoginPluginResponse;
import org.adde0109.ambassador.forge.ForgeHandshakeUtils;

import javax.annotation.Nullable;

public interface VelocityForgeClientConnectionPhase extends ClientConnectionPhase {

  default void handleLogin(@Nullable ForgeHandshakeUtils.CachedServerHandshake initialHandshake) {
  }
  default void handle(LoginPluginResponse packet) {
  }
}
