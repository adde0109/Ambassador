package org.adde0109.ambassador;

import com.electronwill.nightconfig.core.conversion.InvalidValueException;
import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.google.gson.annotations.Expose;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

public class AmbassadorConfig {

  @Expose
  private int serverSwitchCancellationTime = 30;

  @Expose
  private boolean silenceWarnings = false;
  @Expose
  private boolean bypassRegistryCheck = false;
  @Expose
  private boolean bypassModCheck = false;

  @Expose
  private boolean debugMode = false;

  @Expose
  private boolean enableKickReset = false;

  @Expose
  private String kickReconnectMessageString = "<red>Please reconnect.</red>";

  private AmbassadorConfig(boolean silenceWarnings, boolean bypassRegistryCheck, boolean bypassModCheck,
                           boolean debugMode, boolean enableKickReset, String kickReconnectMessageString) {
    this.silenceWarnings = silenceWarnings;
    this.bypassRegistryCheck = bypassRegistryCheck;
    this.bypassModCheck = bypassModCheck;
    this.debugMode = debugMode;
    this.enableKickReset = enableKickReset;
    this.kickReconnectMessageString = kickReconnectMessageString;
  };

  public static AmbassadorConfig read(Path path) throws IOException {
    URL defaultConfigLocation = AmbassadorConfig.class.getClassLoader()
            .getResource("default-ambassador.toml");
    if (defaultConfigLocation == null) {
      throw new RuntimeException("Default configuration file does not exist.");
    }

    CommentedFileConfig config = CommentedFileConfig.builder(path)
            .defaultData(defaultConfigLocation)
            .autosave()
            .preserveInsertionOrder()
            .sync()
            .build();
    config.load();

    double configVersion;
    try {
      configVersion = Double.parseDouble(config.getOrElse("config-version", "1.0"));
    } catch (NumberFormatException e) {
      configVersion = 1.0;
    }

    boolean silenceWarnings = config.getOrElse("silence-warnings", false);

    String kickReconnectMessageString = config.getOrElse("disconnect-reset-message",
            "<red>Please reconnect.</red>");

    //Upgrade config
    if (configVersion <= 1.2) {
      Files.delete(path);
      config = CommentedFileConfig.builder(path)
              .defaultData(defaultConfigLocation)
              .autosave()
              .preserveInsertionOrder()
              .sync()
              .build();
      config.load();
      config.set("silence-warnings", silenceWarnings);
      config.set("reconnect-message", kickReconnectMessageString);
    }

    int serverSwitchCancellationTime = config.getOrElse("serverRedirectTimeout", 30);

    boolean bypassRegistryCheck = config.getOrElse("bypass-registry-checks", false);

    boolean bypassModCheck = config.getOrElse("bypass-mod-checks", false);

    boolean debugMode = config.getOrElse("debug-mode", false);

    boolean enableKickReset = config.getOrElse("enable-kick-reset", false);

    kickReconnectMessageString = config.getOrElse("reconnect-message",
            "<red>Please reconnect.</red>");

    return new AmbassadorConfig(bypassRegistryCheck, bypassModCheck, silenceWarnings,
            debugMode, enableKickReset, kickReconnectMessageString);
  }

  public int getServerSwitchCancellationTime() {
    return serverSwitchCancellationTime;
  }

  public boolean isSilenceWarnings() {
    return silenceWarnings;
  }

  public boolean isBypassRegistryCheck() {
    return bypassRegistryCheck;
  }

  public boolean isBypassModCheck() {
    return bypassModCheck;
  }

  public boolean isDebugMode() {
    return debugMode;
  }

  public boolean isEnableKickReset() {
    return enableKickReset;
  }

  public String getKickReconnectMessageString() {
    return kickReconnectMessageString;
  }
}
