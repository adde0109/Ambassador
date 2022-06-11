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
      CommentedFileConfig config = CommentedFileConfig.builder(dataDirectory.resolve("forgeServer.toml"))
              .defaultData(Ambassador.class.getClassLoader().getResource("default-ambassador.toml"))
              .autosave()
              .preserveInsertionOrder()
              .sync()
              .build();
      config.load();

      CommentedConfig settingsConfig = config.get("Differentiators");


      Differentiators settings = new Differentiators(settingsConfig);

      logger.info(settings.differentiators.get("758").handshakeServer);

      config.save();
    }
    catch (ParsingException e) {
      logger.error("Config related error: " + e.toString());
      return false;
    }



    //758 - 1.18.2

    //754 - 1.16.5

    forgeServer = server.getServer("lobby");
    return true;
  }

  private static class Differentiators {
    private Map<String,DifferentiatorSettings> differentiators = ImmutableMap.of(
            "758", new DifferentiatorSettings(),
            "754", new DifferentiatorSettings()
    );
    private Differentiators(){
    }

    private Differentiators(CommentedConfig config) {
      if (config != null) {
        Map<String,DifferentiatorSettings> differentiators = new HashMap<>();
        for (UnmodifiableConfig.Entry entry : config.entrySet()) {
          if (entry.getValue() instanceof CommentedConfig) {
            differentiators.put(entry.getKey(),new DifferentiatorSettings(entry.getValue()));
          }
        }
        this.differentiators = ImmutableMap.copyOf(differentiators);
      }
    }
  }

  private static class DifferentiatorSettings {
    private String handshakeServer = "";
    private boolean forced = false;

    private DifferentiatorSettings(){
    }

    private DifferentiatorSettings(CommentedConfig config) {
      if (config != null) {
        this.handshakeServer = config.getOrElse("forge-server", handshakeServer);
        this.forced = config.getOrElse("forced",forced);
      }
    }

  }
}
