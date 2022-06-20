package org.adde0109.ambassador;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.ConfigSpec;
import com.electronwill.nightconfig.core.UnmodifiableConfig;
import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.electronwill.nightconfig.core.io.ParsingException;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;

import ninja.leaping.configurate.commented.SimpleCommentedConfigurationNode;
import org.slf4j.Logger;
import com.electronwill.nightconfig.core.file.FileConfig;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

@Plugin(id = "ambassador", name = "Ambassador", version = "0.1.0-SNAPSHOT", url = "", description = "", authors = {"adde0109"})
public class Ambassador {

  private final ProxyServer server;
  private final Logger logger;
  private final Path dataDirectory;
  private Optional<RegisteredServer> forgeServer;
  private AmbassadorConfig config;

  private static ForgeHandshakeDataHandler forgeHandshakeDataHandler;

  @Inject
  public Ambassador(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory) {
    this.server = server;
    this.logger = logger;
    this.dataDirectory = dataDirectory;
  }

  @Subscribe
  public void onProxyInitialization(ProxyInitializeEvent event) {
    config = AmbassadorConfig.readOrCreateConfig(dataDirectory,server,logger);
    if(config != null) {
      forgeHandshakeDataHandler = new ForgeHandshakeDataHandler(config,logger);
      server.getEventManager().register(this, forgeHandshakeDataHandler);
    }
    else {
      logger.warn("Ambassador will be disabled because of errors");
    }

  }


}
