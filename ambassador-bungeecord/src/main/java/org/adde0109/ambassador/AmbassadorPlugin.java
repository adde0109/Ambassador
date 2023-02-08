package org.adde0109.ambassador;

import net.md_5.bungee.api.plugin.Plugin;
import org.adde0109.ambassador.forgeCommandArgument.Injector;

public class AmbassadorPlugin extends Plugin {
    @Override
    public void onEnable() {
      // You should not put an enable message in your plugin.
      // BungeeCord already does so
      getLogger().info("Yay! It loads!");
      Injector.inject();
    }
  }

