package org.adde0109.ambassador.forge;

import com.velocitypowered.api.event.Continuation;
import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.ServerPreConnectEvent;
import com.velocitypowered.api.util.GameProfile;

import java.util.*;
import java.util.concurrent.TimeUnit;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.adde0109.ambassador.Ambassador;

import org.apache.commons.collections4.map.PassiveExpiringMap;

public class ForgeServerSwitchHandler {

  private static final int RESYNC_EXPIRY_TIME = 20;
  private final Ambassador ambassador;
  public final PassiveExpiringMap<String,ForgeServerConnection> reSyncMap;

  public ForgeServerSwitchHandler(Ambassador ambassador) {
    this.ambassador = ambassador;
    this.reSyncMap = new PassiveExpiringMap<>(RESYNC_EXPIRY_TIME,TimeUnit.SECONDS);
  }


  @Subscribe(order = PostOrder.LAST)
  public void onServerPreConnectEvent(ServerPreConnectEvent event, Continuation continuation) {
    if (!event.getResult().isAllowed()) {
      continuation.resume();
      return;
    }
    Optional<ForgeServerConnection> forgeServerConnectionOptional = ambassador.forgeHandshakeHandler.getForgeServerConnection(event.getOriginalServer());
    Optional<ForgeConnection> forgeConnection = ambassador.forgeHandshakeHandler.getForgeConnection(event.getPlayer());
    if (forgeConnection.isPresent()) {
      ForgeServerConnection forgeServerConnection = forgeServerConnectionOptional.orElseGet(() -> new ForgeServerConnection(event.getOriginalServer()));
      forgeServerConnection.getHandshake().whenComplete((msg, ex) -> {
        if (ex != null) {
          //The server was forge but aren't right now. Or it's just offline.
          if (ex instanceof ForgeHandshakeUtils.HandshakeReceiver.HandshakeNotAvailableException) {
            //It's not running ambassador, so it should be unregistered.
            if (forgeServerConnectionOptional.isPresent())
              ambassador.forgeHandshakeHandler.unRegisterForgeServer(forgeServerConnection.getServer());
          }
        } else {
          //If the server just got discovered, register it.
          if (forgeServerConnectionOptional.isEmpty())
            ambassador.forgeHandshakeHandler.registerForgeServer(event.getOriginalServer(),forgeServerConnection);

          //To make legacy forwarding work
          List<GameProfile.Property> properties = new ArrayList<>(event.getPlayer().getGameProfileProperties());
          properties.add(new GameProfile.Property("extraData", "\1FML2\1",""));
          event.getPlayer().setGameProfileProperties(properties);

          if (!msg.equals(forgeConnection.get().getTransmittedHandshake().get())) {
            event.setResult(ServerPreConnectEvent.ServerResult.denied());
            ambassador.logger.info("Kicking {} because of re-sync needed", event.getPlayer());
            event.getPlayer().disconnect(Component.text("Please reconnect"));
            reSyncMap.put(event.getPlayer().getUsername(),forgeServerConnection);
          }
        }
        continuation.resume();
      });
    } else if (forgeServerConnectionOptional.isPresent()) {
      //If vanilla tries to connect to a server we know is forge
      event.setResult(ServerPreConnectEvent.ServerResult.denied());
      event.getPlayer().sendMessage(Component.text("This server requires Forge!", NamedTextColor.RED));
      continuation.resume();
    } else {
      //The server is not known to us.
      continuation.resume();
    }
  }
}
