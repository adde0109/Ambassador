package org.adde0109.ambassador.forge;

import com.velocitypowered.api.event.Continuation;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.connection.MinecraftConnection;
import com.velocitypowered.proxy.connection.backend.VelocityServerConnection;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import com.velocitypowered.proxy.protocol.packet.LoginPluginMessage;
import com.velocitypowered.proxy.protocol.packet.LoginPluginResponse;
import io.netty.buffer.ByteBuf;
import net.kyori.adventure.text.Component;
import org.adde0109.ambassador.Ambassador;
import org.adde0109.ambassador.velocity.VelocityForgeClientConnectionPhase;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class FML2ClientConnectionPhase extends VelocityForgeClientConnectionPhase {

  private Throwable throwable;
  private RegisteredServer triedServer;
  private CompletableFuture<Void> onServerSuccess;
  private CompletableFuture<Void> onJoinGame;

  private static final Method CONNECT_TO_INITIAL_SERVER;

  static {
    Class clazz;
    try {
      clazz = Class.forName("com.velocitypowered.proxy.connection.client.LoginSessionHandler");
    } catch (ClassNotFoundException ignored){
      try {
        clazz = Class.forName("com.velocitypowered.proxy.connection.client.AuthSessionHandler");
      } catch (ClassNotFoundException e) {
        throw new RuntimeException(e);
      }
    }
    try {
      CONNECT_TO_INITIAL_SERVER = clazz.getDeclaredMethod("connectToInitialServer", ConnectedPlayer.class);
      CONNECT_TO_INITIAL_SERVER.setAccessible(true);
    } catch (ReflectiveOperationException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public RegisteredServer chooseServer(ConnectedPlayer player) {
    forced = Ambassador.getTemporaryForced().remove(player.getUsername());
    return forced;
  }

  @Override
  public CompletableFuture<Boolean> reset(RegisteredServer server, ConnectedPlayer player) {
    FML2CRPMClientConnectionPhase newPhase = new FML2CRPMClientConnectionPhase(clientPhase,getPayloadManager());
    player.setPhase(newPhase);
    CompletableFuture<Boolean> future = newPhase.reset(server,player);
    future.thenAccept(success -> {
      if (!success) {
        Ambassador.getTemporaryForced().put(player.getUsername(),server, Ambassador.getInstance().config.getServerSwitchCancellationTime(), TimeUnit.SECONDS);
        player.disconnect(Component.text(Ambassador.getInstance().config.getDisconnectResetMessage()));
      }
    });
    return future;
  }


  public void handleJoinGame() {
      this.onJoinGame.complete(null);
  }

  public CompletableFuture<Void> awaitJoinGame() {
      return this.onJoinGame;
  }

  @Override
  public void handleForward(VelocityServerConnection serverConnection, LoginPluginMessage payload) {
    final ByteBuf buf = payload.content().duplicate();
    ProtocolUtils.readString(buf);  //Channel
    ProtocolUtils.readVarInt(buf);  //Length
    if (ProtocolUtils.readVarInt(buf) == 1) {
      getPayloadManager().listenFor(payload.getId()).thenAccept(rawResponse -> {
        final ByteBuf response = rawResponse.duplicate();
        ProtocolUtils.readString(response);  //Channel
        ProtocolUtils.readVarInt(response);  //Length
        if (ProtocolUtils.readVarInt(response) == 2) {
          String[] mods = ProtocolUtils.readStringArray(response);
          if (Arrays.stream(mods).anyMatch(s -> s.equals("clientresetpacket"))) {
            serverConnection.getPlayer().setPhase(new FML2CRPMClientConnectionPhase(clientPhase,getPayloadManager()));
          }
        }
      });
    }
  }
}
