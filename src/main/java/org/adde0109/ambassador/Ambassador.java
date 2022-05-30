package org.adde0109.ambassador;

import com.google.common.io.ByteArrayDataInput;
import com.google.inject.Inject;
import com.velocitypowered.api.event.Continuation;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PreLoginEvent;
import com.velocitypowered.api.event.player.ServerLoginPluginMessageEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.LoginPhaseConnection;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.messages.ChannelIdentifier;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerPing;
import com.velocitypowered.api.util.ModInfo;
import java.io.EOFException;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.*;

@Plugin(id = "ambassador", name = "Ambassador", version = "0.1.0-SNAPSHOT", url = "", description = "", authors = {"adde0109"})
public class Ambassador {

  private final ProxyServer server;
  private final Logger logger;
  private final Path dataDirectory;
  private RegisteredServer forgeServer;

  private static ForgeHandshakeDataHandler forgeHandshakeDataHandler;

  @Inject
  public Ambassador(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory) {
    this.server = server;
    this.logger = logger;
    this.dataDirectory = dataDirectory;
  }

  @Subscribe
  public void onProxyInitialization(ProxyInitializeEvent event) {
    Path serverFilePath = dataDirectory.resolve("forgeServer.txt");
    /*try {
      Files.createDirectory(dataDirectory);
      serverFilePath.toFile().createNewFile()
    }
    catch (IOException e) {
    }

   */

    forgeServer = server.getServer("lobby").orElseThrow(IllegalAccessError::new);

    forgeHandshakeDataHandler = new ForgeHandshakeDataHandler(forgeServer,logger);
    server.getEventManager().register(this, forgeHandshakeDataHandler);
  }

}
