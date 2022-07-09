package org.adde0109.ambassador.forge;

import com.google.common.io.ByteArrayDataInput;
import com.velocitypowered.api.event.Continuation;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PreLoginEvent;
import com.velocitypowered.api.event.player.KickedFromServerEvent;
import com.velocitypowered.api.event.player.ServerLoginPluginMessageEvent;
import com.velocitypowered.api.proxy.LoginPhaseConnection;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.messages.ChannelIdentifier;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import java.io.EOFException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.adde0109.ambassador.AmbassadorConfig;
import org.slf4j.Logger;

public class ForgeHandshakeHandler {

  private final AmbassadorConfig config;
  private final ProxyServer server;
  private final Logger logger;

  private final Map<RegisteredServer, ForgeServerConnection>
      forgeServerConnectionMap = new HashMap<>();
  private final Map<InetSocketAddress,ForgeConnection> incomingForgeConnections = new HashMap<>();

  private final List<Player> doomedConnections = new ArrayList<>();
  private static final ChannelIdentifier LOGIN_WRAPPER_ID = MinecraftChannelIdentifier.create("fml","loginwrapper");


  public ForgeHandshakeHandler(AmbassadorConfig config, ProxyServer server, Logger logger) {
    this.config = config;
    this.server = server;
    this.logger = logger;
  }

  @Subscribe
  public void onPreLoginEvent(PreLoginEvent event, Continuation continuation) {
    if (!config.shouldHandle(event.getConnection().getProtocolVersion().getProtocol())) {
      continuation.resume();
      return;
    }
    RegisteredServer defaultServer = config.getServer(event.getConnection().getProtocolVersion().getProtocol());

          if (defaultServer == null) {
            continuation.resume();
            return;
          }



          //If a connection does not already exist, create one.
          if (!forgeServerConnectionMap.containsKey(defaultServer)) {
            forgeServerConnectionMap.put(defaultServer, new ForgeServerConnection(defaultServer,logger));
          }

          ForgeServerConnection forgeServerConnection = forgeServerConnectionMap.get(defaultServer);

          //Syncing - continuation is forwarded to this method
          ForgeConnection.sync((LoginPhaseConnection) event.getConnection(),forgeServerConnection,continuation).thenAccept(
              this::onSyncComplete);
  }

  private void onSyncComplete(ForgeConnection forgeConnection) {
    if (forgeConnection != null) {
      incomingForgeConnections.values().removeIf((c) -> !c.getConnection().isActive());
      incomingForgeConnections.put(forgeConnection.getConnection().getRemoteAddress(), forgeConnection);
    }
  }

  public Optional<ForgeConnection> getForgeConnection(Player player) {
    return getForgeConnection(player.getRemoteAddress());
  }

  private Optional<ForgeConnection> getForgeConnection(InetSocketAddress socketAddress) {
    incomingForgeConnections.values().removeIf((c) -> !c.getConnection().isActive());
    return Optional.ofNullable(incomingForgeConnections.get(socketAddress));
  }

  public Optional<ForgeServerConnection> getForgeServerConnection(RegisteredServer registeredServer) {
    return Optional.ofNullable(forgeServerConnectionMap.get(registeredServer));
  }

  public void registerForgeServer(RegisteredServer server, ForgeServerConnection forgeServerConnection) {
    forgeServerConnectionMap.put(server,forgeServerConnection);
  }

  @Subscribe
  public void onServerLoginPluginMessageEvent(ServerLoginPluginMessageEvent event, Continuation continuation) {
    if (!event.getIdentifier().equals(LOGIN_WRAPPER_ID)) {
      continuation.resume();
      return;
    }
    //Check 2
    if (getForgeServerConnection(event.getConnection().getServer()).isEmpty()) {
      registerForgeServer(event.getConnection().getServer(),
          new ForgeServerConnection(event.getConnection().getServer(), logger));
    }

    if (incomingForgeConnections.containsKey(event.getConnection().getPlayer().getRemoteAddress())) {
      incomingForgeConnections.get(event.getConnection().getPlayer().getRemoteAddress())
          .handleServerHandshakePacket(event,continuation);
    } else {
        continuation.resume();
    }
  }

  @Subscribe
  public void onKickedFromServerEvent(KickedFromServerEvent event, Continuation continuation) {
    if (getForgeServerConnection(event.getServer()).isPresent() && getForgeConnection(event.getPlayer()).isEmpty()) {
      //Turns out the server the vanilla client is connecting to is forge. Let's handle the connection error.
      logger.info("Vanilla player {} tried to connect to forge server {}. The connection error can be ignored.",
          event.getPlayer(),event.getServer().getServerInfo().getName());
      KickedFromServerEvent.ServerKickResult result = event.getResult();
      Component component = Component.text("The server you were trying to connect to requires Forge to be installed.", NamedTextColor.RED);
      if (result instanceof KickedFromServerEvent.DisconnectPlayer) {
        event.setResult(KickedFromServerEvent.DisconnectPlayer.create(component));
      } else if (result instanceof KickedFromServerEvent.RedirectPlayer) {
        RegisteredServer redirectServer = ((KickedFromServerEvent.RedirectPlayer)event.getResult()).getServer();
        event.setResult(KickedFromServerEvent.RedirectPlayer.create(redirectServer,component));
      } else if (result instanceof KickedFromServerEvent.Notify) {
        event.setResult(KickedFromServerEvent.Notify.create(component));
      }
    }
    continuation.resume();
  }
}
