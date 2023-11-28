package org.adde0109.ambassador.forge;

import com.velocitypowered.proxy.connection.MinecraftConnection;
import com.velocitypowered.proxy.connection.backend.BackendConnectionPhase;
import com.velocitypowered.proxy.connection.backend.VelocityServerConnection;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import com.velocitypowered.proxy.network.Connections;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import com.velocitypowered.proxy.protocol.packet.AvailableCommands;
import com.velocitypowered.proxy.protocol.packet.LoginPluginMessage;
import com.velocitypowered.proxy.protocol.packet.PluginMessage;
import org.adde0109.ambassador.forge.packet.*;
import org.adde0109.ambassador.forge.pipeline.CommandDecoderErrorCatcher;
import org.adde0109.ambassador.forge.pipeline.ForgeLoginWrapperCodec;
import org.checkerframework.checker.units.qual.A;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.zip.Adler32;
import java.util.zip.Checksum;

public enum VelocityForgeBackendConnectionPhase implements BackendConnectionPhase {
  NOT_STARTED() {
    @Override
    VelocityForgeBackendConnectionPhase nextPhase() {
      return WAITING_FOR_ACK;
    }

    @Override
    public boolean consideredComplete() {
      //Safe if the server hasn't initiated the handshake yet.
      return true;
    }
  },
  WAITING_FOR_ACK() {
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

  COMPLETE() {
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

    //Reset client if not ready to receive new handshake
    VelocityForgeClientConnectionPhase clientPhase = (VelocityForgeClientConnectionPhase) player.getPhase();
    if (clientPhase == VelocityForgeClientConnectionPhase.RESETTABLE) {
      //Initial Forge
      //Forge -> Forge
      clientPhase.resetConnectionPhase(player);
    }

    if (clientPhase != VelocityForgeClientConnectionPhase.COMPLETE) {
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
        CompletableFuture.runAsync(() -> {
          try {
            remainingRegistries.await();
          } catch (InterruptedException e) {
            throw new RuntimeException(e);
          }
        }).thenAcceptAsync((v) -> {
          if (clientPhase.forgeHandshake.isCompatible(handshake)) {
            server.ensureConnected().write(clientPhase.forgeHandshake.getModListReplyPacket());
          } else {
            server.disconnect();
          }
        }, server.ensureConnected().eventLoop());
      } else if (message instanceof RegistryPacket registryPacket) {
        server.getConnection().write(new ACKPacket(Context.createContext(message.getContext().getResponseID(), true)));
        handshake.addRegistry(registryPacket);
        remainingRegistries.countDown();
      } else if (message instanceof ConfigDataPacket) {
        server.getConnection().write(new ACKPacket(Context.createContext(message.getContext().getResponseID(), true)));
      } else if (message instanceof GenericForgeLoginWrapperPacket<?>) {
        //Save for after completion and send as plugin message
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
  public boolean handle(VelocityServerConnection server, ConnectedPlayer player, PluginMessage message) {
    if (message.getChannel().equals("ambassador:commands")) {
      AvailableCommands packet = new AvailableCommands();
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
