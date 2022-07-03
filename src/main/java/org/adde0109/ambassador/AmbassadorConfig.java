package org.adde0109.ambassador;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.UnmodifiableConfig;
import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.google.common.collect.ImmutableMap;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class AmbassadorConfig {


  private final ProxyServer server;
  private final Logger logger;
  private Differentiators settings;


  private AmbassadorConfig(ProxyServer server,Logger logger) {
    this.server = server;
    this.logger = logger;
  }



  public RegisteredServer getServer(int protocolVersion) {
    return settings.differentiators.get(protocolVersion).handshakeServer;
  }

  public boolean getForced (int protocolVersion) {
    return settings.differentiators.get(protocolVersion).forced;
  }

  public boolean shouldHandle(int protocolVersion) {
    return settings.differentiators.containsKey(protocolVersion);
  }



  public static AmbassadorConfig readOrCreateConfig(Path dataDirectory,ProxyServer server, Logger logger) {
    AmbassadorConfig ambassadorConfig = new AmbassadorConfig(server,logger);
    try {
      Files.createDirectories(dataDirectory);

    }
    catch (FileAlreadyExistsException ignored) {

    }
    catch (IOException e) {
      logger.error("Config related error: " + e);
      return null;
    }

    try {
      CommentedFileConfig config = CommentedFileConfig.builder(dataDirectory.resolve("ambassador.toml"))
              .defaultData(Ambassador.class.getClassLoader().getResource("default-ambassador.toml"))
              .autosave()
              .preserveInsertionOrder()
              .sync()
              .build();
      config.load();

      CommentedConfig settingsConfig = config.get("Differentiators");


      ambassadorConfig.settings = ambassadorConfig.new Differentiators(settingsConfig);


      config.save();
      return ambassadorConfig;
    }
    catch (Exception e) {
      logger.error("Config related error: " + e);
      return null;
    }
  }

  private class Differentiators {
    private Map<Integer,DifferentiatorSettings> differentiators = ImmutableMap.of(
            758, new DifferentiatorSettings(),
            754, new DifferentiatorSettings()
    );
    private Differentiators(){
    }

    private Differentiators(CommentedConfig config) throws Exception {
      if (config != null) {
        Map<Integer,DifferentiatorSettings> differentiators = new HashMap<>();
        for (UnmodifiableConfig.Entry entry : config.entrySet()) {
          if (entry.getValue() instanceof CommentedConfig) {
            differentiators.put(Integer.decode(entry.getKey()),new DifferentiatorSettings(entry.getValue()));
          }
        }
        this.differentiators = ImmutableMap.copyOf(differentiators);
      }
    }
  }

  private class DifferentiatorSettings {
    private RegisteredServer handshakeServer = null;
    private boolean forced = false;

    private DifferentiatorSettings(){
    }

    private DifferentiatorSettings(CommentedConfig config) throws Exception {
      if (config != null) {
        String serverName = config.getOrElse("forge-server", "");
        if (!Objects.equals(serverName, ""))
          handshakeServer = server.getServer(serverName)
                .orElseThrow(() -> new Exception(serverName + "is not a registered server!"));
        this.forced = config.getOrElse("forced",forced);
      }
    }

  }
}
