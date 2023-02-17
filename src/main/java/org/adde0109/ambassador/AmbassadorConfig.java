package org.adde0109.ambassador;

import com.electronwill.nightconfig.core.conversion.InvalidValueException;
import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.google.gson.annotations.Expose;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;

import java.net.URL;
import java.nio.file.Path;

public class AmbassadorConfig {

  @Expose
  private int resetTimeout = 1000;

  @Expose
  private String disconnectResetMessage = "Please reconnect";

  @Expose
  private int serverSwitchCancellationTime = 120;

  private net.kyori.adventure.text.@MonotonicNonNull Component messageAsAsComponent;

  private AmbassadorConfig(int resetTimeout, String kickResetMessage, int serverSwitchCancellationTime) {
    this.resetTimeout = resetTimeout;
    this.disconnectResetMessage = kickResetMessage;
    this.serverSwitchCancellationTime = serverSwitchCancellationTime;
  };

  public void validate() {
    final int connectionTimeout = Ambassador.getInstance().server.getConfiguration().getConnectTimeout();
    if (resetTimeout >= connectionTimeout) {
      throw new InvalidValueException("'reset-timeout' can't be more than nor equal to 'connection-timeout': reset-timeout=" + resetTimeout + " connection-timeout=" + connectionTimeout);
    }
    if (resetTimeout <= 0) {
      throw new InvalidValueException("'reset-timeout' can't be less than nor equal to zero: reset-timeout=" + resetTimeout);
    }
    if (serverSwitchCancellationTime <= 0) {
      throw new InvalidValueException("'server-switch-cancellation-time' can't be less than nor equal to zero: server-switch-cancellation-time=" + serverSwitchCancellationTime);
    }
  }

  public static AmbassadorConfig read(Path path) {
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

    int resetTimeout = config.getIntOrElse("reset-timeout", 3000);
    String kickResetMessage = config.getOrElse("disconnect-reset-message", "Please reconnect");
    int serverSwitchCancellationTime = config.getIntOrElse("server-switch-cancellation-time", 120000);

    return new AmbassadorConfig(resetTimeout, kickResetMessage, serverSwitchCancellationTime);
  }

  public int getResetTimeout() {
    return resetTimeout;
  }

  public net.kyori.adventure.text.Component getDisconnectResetMessage() {
    if (messageAsAsComponent == null) {
      if (disconnectResetMessage.startsWith("{")) {
        messageAsAsComponent = GsonComponentSerializer.gson().deserialize(disconnectResetMessage);
      } else {
        messageAsAsComponent = LegacyComponentSerializer.legacyAmpersand().deserialize(disconnectResetMessage);
      }
    }
    return messageAsAsComponent;
  }

  public int getServerSwitchCancellationTime() {
    return serverSwitchCancellationTime;
  }
}
