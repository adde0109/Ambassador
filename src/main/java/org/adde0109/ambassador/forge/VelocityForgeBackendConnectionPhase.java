package org.adde0109.ambassador.forge;

import com.velocitypowered.proxy.connection.MinecraftConnection;
import com.velocitypowered.proxy.connection.backend.BackendConnectionPhase;
import com.velocitypowered.proxy.connection.backend.VelocityServerConnection;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import com.velocitypowered.proxy.network.Connections;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import com.velocitypowered.proxy.protocol.StateRegistry;
import com.velocitypowered.proxy.protocol.packet.AvailableCommandsPacket;
import com.velocitypowered.proxy.protocol.packet.PluginMessagePacket;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.text.Component;
import org.adde0109.ambassador.Ambassador;
import org.adde0109.ambassador.forge.packet.*;
import org.adde0109.ambassador.forge.pipeline.CommandDecoderErrorCatcher;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;

public enum VelocityForgeBackendConnectionPhase implements BackendConnectionPhase {
  NOT_STARTED {
    @Override
    VelocityForgeBackendConnectionPhase nextPhase() {
      return IN_PROGRESS;
    }

    @Override
    public boolean consideredComplete() {
      //Safe if the server hasn't initiated the handshake yet.
      return true;
    }
  },
  IN_PROGRESS {
    @Override
    public void onLoginSuccess(VelocityServerConnection serverCon, ConnectedPlayer player) {
      serverCon.setConnectionPhase(VelocityForgeBackendConnectionPhase.COMPLETE);

      serverCon.getConnection().getChannel().pipeline().addBefore(Connections.MINECRAFT_DECODER,
              ForgeConstants.COMMAND_ERROR_CATCHER,
              new CommandDecoderErrorCatcher(serverCon.getConnection().getProtocolVersion(),player));
    }

    @Override
    void onTransitionToNewPhase(VelocityServerConnection connection) {
      MinecraftConnection mc = connection.getConnection();
      if (mc != null) {
        //This looks ugly. But unless the player didn't have a FML marker, we're fine.
        mc.setType(connection.getPlayer().getConnection().getType());
      }
    }
  },

  COMPLETE {
    @Override
    public boolean consideredComplete() {
      return true;
    }
  };

  public ForgeHandshake handshake = new ForgeHandshake();
  CountDownLatch remainingRegistries;

  VelocityForgeBackendConnectionPhase() {
  }

  public void handle(VelocityServerConnection server, ConnectedPlayer player, IForgeLoginWrapperPacket message) {
    VelocityForgeBackendConnectionPhase newPhase = getNewPhase(server,message);

    server.setConnectionPhase(newPhase);

    if (player.getPhase() == VelocityForgeClientConnectionPhase.NOT_STARTED ||
            player.getPhase() == VelocityForgeClientConnectionPhase.IN_PROGRESS) {
      //Initial Forge
      player.getConnection().write(message);
      return;
    }

    //Forge -> Forge

    //Reset client if not ready to receive new handshake
    VelocityForgeClientConnectionPhase clientPhase = (VelocityForgeClientConnectionPhase) player.getPhase();
    if (clientPhase.getResetType() == VelocityForgeClientConnectionPhase.clientResetType.CRP ||
            clientPhase.getResetType() == VelocityForgeClientConnectionPhase.clientResetType.SR) {
      clientPhase.resetConnectionPhase(player);
      player.getConnection().write(message);
      return;
    }


    //STILL WIP
    if (!Ambassador.getInstance().config.isDebugMode()) {
      server.disconnect();
      return;
    }

    if (clientPhase.getResetType() == VelocityForgeClientConnectionPhase.clientResetType.NONE) {
      if (message instanceof ModListPacket modListPacket) {
        clientPhase.forgeHandshake = new ForgeHandshake();
      }
      if (message instanceof RegistryPacket registryPacket) {
        clientPhase.forgeHandshake.addRegistry(registryPacket);
      }
      player.getConnection().write(message);
    } else {
      if (message instanceof ModListPacket modListPacket) {
        remainingRegistries = new CountDownLatch(modListPacket.getRegistries().size());

        if (Ambassador.getInstance().config.isDebugMode())
          player.sendMessage(Component.text("Expecting " + modListPacket.getRegistries().size() +
                  " packets from server " + server.getServer().getServerInfo().getName()));

        long time = System.currentTimeMillis();
        CompletableFuture.runAsync(() -> {
          try {
            remainingRegistries.await();
          } catch (InterruptedException e) {
            throw new RuntimeException(e);
          }
        }).thenAcceptAsync((v) -> {

          if(Ambassador.getInstance().config.isDebugMode()) {
            player.sendMessage(Component.text("Handshake took: " + (System.currentTimeMillis()-time)/1000 + " seconds"));
            player.sendMessage(Component.text("Avg packet time" +
                    ((System.currentTimeMillis()-time)/1000)/modListPacket.getRegistries().size() + " seconds"));
          }

          if (Ambassador.getInstance().config.isBypassRegistryCheck() ||
                  clientPhase.forgeHandshake.isCompatible(handshake)) {
            server.ensureConnected().write(clientPhase.forgeHandshake.getModListReplyPacket());
          } else {
            Ambassador.getInstance().logger.error("Unable to switch due to the registries of " +
                    server.getServer().getServerInfo().getName() + " being different from the registries of " +
                    player.getConnectedServer().getServer().getServerInfo().getName());
            server.disconnect();
          }
        }, server.ensureConnected().eventLoop());
      } else if (message instanceof RegistryPacket registryPacket) {
        server.getConnection().write(new ACKPacket(Context.fromContext(message.getContext(), true)));
        handshake.addRegistry(registryPacket);
        remainingRegistries.countDown();
      } else if (message instanceof ConfigDataPacket) {
        server.getConnection().write(new ACKPacket(Context.fromContext(message.getContext(), true)));
      } else if (message instanceof GenericForgeLoginWrapperPacket<?> packet
              && ForgeHandshakeUtils.SilentGearUtils.isSilentGearPacket(packet.getContent())) {
        server.getConnection().write(new ForgeHandshakeUtils.SilentGearUtils.ACKPacket(
                Context.fromContext(message.getContext(), true)));
      }
    }
    //Forge server
    //To avoid unnecessary resets, we wait until we get the handshake even if we know that we should
    //reset because that the previous server was Forge.
  }

  public void onLoginSuccess(VelocityServerConnection serverCon, ConnectedPlayer player) {
  }

  void onTransitionToNewPhase(VelocityServerConnection connection) {
  }

  VelocityForgeBackendConnectionPhase nextPhase() {
    return this;
  }

  private VelocityForgeBackendConnectionPhase getNewPhase(VelocityServerConnection serverConnection,
                                                       IForgeLoginWrapperPacket<Context> packet) {
    VelocityForgeBackendConnectionPhase phaseToTransitionTo = nextPhase();
    if (phaseToTransitionTo != this) {
      phaseToTransitionTo.onTransitionToNewPhase(serverConnection);
    }
    return phaseToTransitionTo;
  }

  @Override
  public boolean handle(VelocityServerConnection server, ConnectedPlayer player, PluginMessagePacket message) {
    if (message.getChannel().equals("ambassador:commands")) {
      AvailableCommandsPacket packet = new AvailableCommandsPacket();
      packet.decode(message.content(), ProtocolUtils.Direction.CLIENTBOUND,server.getConnection().getProtocolVersion());
      server.getConnection().getActiveSessionHandler().handle(packet);
      return true;
    }
    return false;
  }

  public boolean consideredComplete() {
    return false;
  }



}
