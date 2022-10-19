package org.adde0109.ambassador.forge;

import com.velocitypowered.api.event.Continuation;
import com.velocitypowered.api.event.player.PlayerChooseInitialServerEvent;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerPing;
import com.velocitypowered.api.util.ModInfo;
import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.connection.MinecraftConnection;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.connection.backend.VelocityServerConnection;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import com.velocitypowered.proxy.connection.client.LoginSessionHandler;
import com.velocitypowered.proxy.protocol.packet.LoginPluginResponse;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.adde0109.ambassador.velocity.VelocityForgeClientConnectionPhase;
import org.adde0109.ambassador.velocity.VelocityLoginPayloadManager;
import org.apache.commons.collections4.map.PassiveExpiringMap;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

public class FML2ClientConnectionPhase extends VelocityForgeClientConnectionPhase {

  private static final PassiveExpiringMap<String,RegisteredServer> TEMPORARY_FORCED = new PassiveExpiringMap<>(120, TimeUnit.SECONDS);

  private Throwable throwable;
  private RegisteredServer triedServer;
  private Continuation continuation;


  @Override
  public void handleLogin(ConnectedPlayer player, VelocityServer server, Continuation continuation) {
    this.continuation = continuation;
    final MinecraftConnection connection = player.getConnection();

    final Runnable defaultTask = () -> {
      Optional<RegisteredServer> initialFromConfig = player.getNextServerToTry();
      PlayerChooseInitialServerEvent event = new PlayerChooseInitialServerEvent(player,
              initialFromConfig.orElse(null));
      server.getEventManager().fire(event)
              .thenRun(() -> {
                Optional<RegisteredServer> toTry = event.getInitialServer();
                tryServer(player, toTry.orElse(null));
              });
    };

    final RegisteredServer forced = TEMPORARY_FORCED.remove(player.getUsername());
    if (forced != null) {
      forced.ping().whenCompleteAsync((msg,ex) -> {
        if (ex == null) {
          if (throwable == null)
            throwable = ex;
          handlePingResponse(msg);
        } else {
          defaultTask.run();
        }
      },connection.eventLoop());
    } else {
      connection.eventLoop().submit(defaultTask);
    }
  }

  @Override
  public void reset(VelocityServerConnection serverConnection, ConnectedPlayer player, Runnable whenComplete) {
    TEMPORARY_FORCED.put(player.getUsername(),serverConnection.getServer());
    player.disconnect(Component.text("Please reconnect"));
  }

  @Override
  public void complete(VelocityServer server, ConnectedPlayer player, MinecraftConnection connection) {
    if (triedServer != null)
      player.sendMessage(Component.translatable("velocity.error.connecting-server-error",
              Component.text(triedServer.getServerInfo().getName())));
  }

  private void tryServer(ConnectedPlayer player, RegisteredServer server) {
    if (server == null) {
      player.disconnect0(Component.translatable("velocity.error.no-available-servers",
              NamedTextColor.RED), true);
      return;
    }
    server.ping().whenCompleteAsync((msg,ex) -> {
      if (ex != null) {
        if (throwable == null)
          throwable = ex;
        tryServer(player,player.getNextServerToTry().orElse(null));
      } else {
        handlePingResponse(msg);
      }
    }, player.getConnection().eventLoop());
    }

  private void handlePingResponse(ServerPing ping) {
    if (ping.getModinfo().isEmpty() || !ping.getModinfo().get().getType().equals("Ambassador")) {
      continuation.resume();
      return;
    }
    ModInfo.Mod mod = ping.getModinfo().get().getMods().get(0);
    String data = mod.getId();
    String markers = mod.getVersion();

  }
}
