package org.adde0109.ambassador.forge;

import com.velocitypowered.api.event.Continuation;
import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.ServerPreConnectEvent;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.scheduler.ScheduledTask;
import com.velocitypowered.api.scheduler.TaskStatus;
import com.velocitypowered.api.util.GameProfile;

import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.adde0109.ambassador.Ambassador;

public class ForgeServerSwitchHandler {

  private final Ambassador ambassador;
  public final ReSyncTracker reSyncTracker;

  public ForgeServerSwitchHandler(Ambassador ambassador) {
    this.ambassador = ambassador;
    this.reSyncTracker = new ReSyncTracker();
  }


  @Subscribe(order = PostOrder.LAST)
  public void onServerPreConnectEvent(ServerPreConnectEvent event, Continuation continuation) {
    if (!event.getResult().isAllowed()) {
      continuation.resume();
      return;
    }
    Optional<ForgeServerConnection> forgeServerConnectionOptional = ambassador.forgeHandshakeHandler.getForgeServerConnection(event.getOriginalServer());
    if (forgeServerConnectionOptional.isPresent()) {
      //Check 1; Check if the server is already known to us. Check if the client is compatible.
      ForgeServerConnection forgeServerConnection = forgeServerConnectionOptional.get();
      forgeServerConnection.getHandshake().whenComplete((msg, ex) -> {
        if (ex != null) {
          //The server was forge but aren't right now. Or it's just offline.
          if (ex instanceof ForgeHandshakeUtils.HandshakeReceiver.HandshakeNotAvailableException) {
            //It's not running ambassador, so it should be unregistered.
            ambassador.forgeHandshakeHandler.unRegisterForgeServer(forgeServerConnection.getServer());
          }
          continuation.resume();
        } else {
          Optional<ForgeConnection> forgeConnection = ambassador.forgeHandshakeHandler.getForgeConnection(event.getPlayer());
          if (forgeConnection.isEmpty() && (event.getPlayer().getCurrentServer().isPresent())) {
            //If vanilla tries to connect to a server we know is forge
            event.setResult(ServerPreConnectEvent.ServerResult.denied());
            event.getPlayer().sendMessage(Component.text("This server requires Forge!", NamedTextColor.RED));
            continuation.resume();
          } else if (forgeConnection.isPresent()) {

            //To make legacy forwarding work
            List<GameProfile.Property> properties = new ArrayList<>(event.getPlayer().getGameProfileProperties());
            properties.add(new GameProfile.Property("extraData", "\1FML2\1",""));
            event.getPlayer().setGameProfileProperties(properties);

            if (forgeConnection.get().getTransmittedHandshake().isPresent()
                && forgeConnection.get().getRecivedClientModlist().isPresent()
                && msg.equals(forgeConnection.get().getTransmittedHandshake().get())) {
              //The client's registry is the same as the server's
              continuation.resume();
            } else {
              event.setResult(ServerPreConnectEvent.ServerResult.denied());
              ambassador.logger.info("Kicking {} because of re-sync needed", event.getPlayer());
              event.getPlayer().disconnect(Component.text("Please reconnect"));
              reSyncTracker.put(event.getPlayer().getRemoteAddress(),event.getOriginalServer());
              continuation.resume();
            }
          } else {
            //If the initial server is forge while the client is vanilla.
            //Can't handle, just let it pass.
            continuation.resume();
          }
        }
      });
    } else {
      //The server is not known to us.
      continuation.resume();
    }
  }
  class ReSyncTracker {
    private static final int TIMEOUT = 2;
    private static final int TICK_TIME = 15;
    private Optional<ScheduledTask> scheduledTask = Optional.empty();
    private final AtomicInteger counter = new AtomicInteger();
    private Map<InetSocketAddress, Integer> reSyncTimeoutMap = new HashMap<>();
    private Map<InetSocketAddress,RegisteredServer> reSyncTargetMap = new HashMap<>();

    public void tick() {
      int c = counter.incrementAndGet();
      if (reSyncTimeoutMap.values().removeIf((v) -> c>=v))
        reSyncTargetMap.keySet().removeIf((k) -> !reSyncTargetMap.containsKey(k));
      //Remove if the reSyncTargetMap is empty
      if (reSyncTargetMap.isEmpty() && scheduledTask.isPresent())
        if (scheduledTask.get().status() == TaskStatus.SCHEDULED)
          scheduledTask = Optional.empty();
    }

    public void put(InetSocketAddress inetSocketAddress, RegisteredServer registeredServer) {
      reSyncTimeoutMap.put(inetSocketAddress, counter.get()+TIMEOUT);
      reSyncTargetMap.put(inetSocketAddress, registeredServer);
      //Start if not already started
      if (scheduledTask.isPresent())
        if (scheduledTask.get().status() == TaskStatus.CANCELLED || scheduledTask.get().status() == TaskStatus.FINISHED)
          scheduledTask = Optional.of(ambassador.server.getScheduler().buildTask((ambassador), this::tick)
                  .repeat(TICK_TIME, TimeUnit.SECONDS).schedule());
    }
    public RegisteredServer remove(InetSocketAddress inetSocketAddress) {
      reSyncTimeoutMap.remove(inetSocketAddress);
      return reSyncTargetMap.remove(inetSocketAddress);
    }
  }
}
