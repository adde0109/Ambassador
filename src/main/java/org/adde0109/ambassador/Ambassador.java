package org.adde0109.ambassador;

import com.electronwill.nightconfig.core.ConfigSpec;
import com.electronwill.nightconfig.core.io.ParsingException;
import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;

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
  private FileConfig config;

  private static ForgeHandshakeDataHandler forgeHandshakeDataHandler;

  @Inject
  public Ambassador(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory) {
    this.server = server;
    this.logger = logger;
    this.dataDirectory = dataDirectory;
  }

  @Subscribe
  public void onProxyInitialization(ProxyInitializeEvent event) {
    if(readOrCreateConfig()) {
      forgeHandshakeDataHandler = new ForgeHandshakeDataHandler(forgeServer.get(),logger);
      server.getEventManager().register(this, forgeHandshakeDataHandler);
    }
    else {
      logger.info("Ambassador will be disabled because of errors");
    }

  }

  private boolean readOrCreateConfig() {
    try {
      Files.createDirectories(dataDirectory);
      Files.createFile(dataDirectory.resolve("forgeServer.toml"));

    }
    catch (FileAlreadyExistsException ignored) {

    }
    catch (IOException e) {
      logger.error("Config related error: " + e.toString());
      return false;
    }

    try {
      config = FileConfig.of(dataDirectory.resolve("forgeServer.toml"));
      config.load();
    }
    catch (ParsingException e) {
      logger.error("Config related error: " + e.toString());
      return false;
    }


    ConfigSpec spec = new ConfigSpec();
    spec.define("Forge Server", "");

    spec.correct(config);

    config.save();
    if(config.get("Forge Server") != "") {
      forgeServer = server.getServer(config.get("Forge Server"));
      if(!forgeServer.isPresent()) {
        logger.error("Could not find " + config.get("Forge Server") + " in registered servers!");
        config.close();
        return false;
      }
    }
    else {
      logger.error("Please specify the forge server in the config");
      config.close();
      return false;
    }
    return true;
  }

}
