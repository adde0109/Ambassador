package org.adde0109.ambassador.forge;

import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import com.velocitypowered.api.util.ModInfo;
import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.connection.MinecraftConnection;
import com.velocitypowered.proxy.connection.backend.VelocityServerConnection;
import com.velocitypowered.proxy.connection.client.ClientConnectionPhase;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import com.velocitypowered.proxy.network.Connections;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import com.velocitypowered.proxy.protocol.StateRegistry;
import com.velocitypowered.proxy.protocol.packet.LoginPluginMessagePacket;
import com.velocitypowered.proxy.protocol.packet.PluginMessage;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.kyori.adventure.text.Component;
import org.adde0109.ambassador.Ambassador;
import org.adde0109.ambassador.forge.packet.Context;
import org.adde0109.ambassador.forge.packet.IForgeLoginWrapperPacket;
import org.adde0109.ambassador.forge.packet.ModListReplyPacket;
import org.adde0109.ambassador.velocity.client.FML2CRPMResetCompleteDecoder;
import org.adde0109.ambassador.velocity.client.OutboundSuccessHolder;
import org.adde0109.ambassador.velocity.client.ClientPacketQueue;

import java.nio.charset.StandardCharsets;

public enum VelocityForgeClientConnectionPhase implements ClientConnectionPhase {

  NOT_STARTED {
    @Override
    VelocityForgeClientConnectionPhase nextPhase() {
      return IN_PROGRESS;
    }

    @Override
    public void complete(ConnectedPlayer player) {
      //When no handshake has taken place.
      //Test if the client supports CRP.
      clientResetType.CRP.doReset(player);
    }

    @Override
    public boolean consideredComplete() {
      return true;
    }
    },
  IN_PROGRESS {
  },
  WAITING_RESET {
    @Override
    void onTransitionToNewPhase(ConnectedPlayer player) {
      //We unregister so no plugin sees this client while the client is being reset.
      ((VelocityServer) Ambassador.getInstance().server).unregisterConnection(player);
      player.getConnection().getChannel().pipeline().addAfter(Connections.MINECRAFT_ENCODER,
              ForgeConstants.LOGIN_PACKET_QUEUE, new ClientPacketQueue(StateRegistry.PLAY));
      if (player.getConnection().getChannel().pipeline().get(ForgeConstants.PLUGIN_PACKET_QUEUE) == null)
        player.getConnection().getChannel().pipeline().addAfter(Connections.MINECRAFT_ENCODER,
                ForgeConstants.PLUGIN_PACKET_QUEUE, new ClientPacketQueue(StateRegistry.LOGIN));
    }

    @Override
    public boolean handle(ConnectedPlayer player, IForgeLoginWrapperPacket msg, VelocityServerConnection server) {
      if (msg.getContext().getResponseID() == 98) {
        //Reset complete
        player.getConnection().getChannel().pipeline().remove(ForgeConstants.RESET_LISTENER);
        player.setPhase(NOT_STARTED);

        player.getConnection().getChannel().pipeline().remove(ForgeConstants.LOGIN_PACKET_QUEUE);

        if (!(server.getConnection().getType() instanceof ForgeFMLConnectionType)) {
          // -> vanilla
          complete(player, ((Context.ClientContext) msg.getContext()).success() ? clientResetType.CRP : clientResetType.UNKNOWN);
        }

        if (player.getConnectionInFlight() != null) {
          player.getConnectionInFlight().getConnection().getChannel().config().setAutoRead(true);
        }

        return true;
      } else {
        return false;
      }
    }
  },
  COMPLETE {
    @Override
    void onTransitionToNewPhase(ConnectedPlayer player) {
      //Plugins may now send packets to client
      player.getConnection().getChannel().pipeline().remove(ForgeConstants.PLUGIN_PACKET_QUEUE);
      ((VelocityServer) Ambassador.getInstance().server).registerConnection(player);
    }

    @Override
    public void resetConnectionPhase(ConnectedPlayer player) {
      getResetType().doReset(player);
    }

    @Override
    public boolean consideredComplete() {
      return true;
    }

    public void complete(ConnectedPlayer player, boolean resettable) {  }

  };

  //TODO: Make a new class that's linked to each player with these fields instead of having them in this phase class
  public ForgeHandshake forgeHandshake = new ForgeHandshake();

  private clientResetType resetType = clientResetType.UNKNOWN;

  public boolean handle(ConnectedPlayer player, IForgeLoginWrapperPacket<Context.ClientContext> msg, VelocityServerConnection server) {

    if (msg instanceof ModListReplyPacket replyPacket) {
      ModInfo modInfo = new ModInfo("FML2", replyPacket.getMods().stream().map(
              (v) -> new ModInfo.Mod(v,"1")).toList());
      player.setModInfo(modInfo);
      forgeHandshake.setModListReplyPacket(replyPacket);
      if (!(server.getConnection().getType() instanceof ForgeFMLConnectionType)) {
        complete(player);
        player.getConnectionInFlight().getConnection().getChannel().config().setAutoRead(true);
        return true;
      }
      replyPacket.getChannels().put(MinecraftChannelIdentifier.from("ambassador:commands"),"1");
    }

    player.getConnectionInFlight().getConnection().write(msg);

    player.setPhase(nextPhase());
    nextPhase().forgeHandshake = this.forgeHandshake;

    return true;
  }

  public void complete(ConnectedPlayer player) {
    complete(player, getResetType(player));
  }

  public void complete(ConnectedPlayer player, clientResetType resetType) {
    MinecraftConnection connection = player.getConnection();
    //Send Login Success to client
    ((OutboundSuccessHolder) connection.getChannel().pipeline().get(ForgeConstants.SERVER_SUCCESS_LISTENER))
            .sendPacket();
    connection.setState(StateRegistry.PLAY);

    //Change phase to COMPLETE
    player.setPhase(COMPLETE);
    COMPLETE.resetType = resetType;
    COMPLETE.onTransitionToNewPhase(player);
    COMPLETE.forgeHandshake = forgeHandshake;

    if (Ambassador.getInstance().config.isDebugMode()) {
      player.sendMessage(Component.text("Forge handshake complete"));
      player.sendMessage(Component.text("Reset type: " + resetType.toString()));
    }
  }

  private clientResetType getResetType(ConnectedPlayer player) {
    if (Ambassador.getInstance().config.isDebugMode()) {
      player.sendMessage(Component.text("Scanning modlist for client reset mods"));
    }
    if (player.getModInfo().isPresent()) {
      if (player.getModInfo().get().getMods().stream().anyMatch((mod -> mod.getId().equals("clientresetpacket")))) {
        return clientResetType.CRP;
      } else if (Ambassador.getInstance().config.getServerSwitchCancellationTime() >= 0 &&
              player.getModInfo().get().getMods().stream().anyMatch((mod -> mod.getId().equals("serverredirect")
              || mod.getId().equals("srvredirect:red")))
              && player.getVirtualHost().isPresent()) {
        return clientResetType.SR;
      }
    }
    return clientResetType.NONE;
  }

  void onTransitionToNewPhase(ConnectedPlayer player) {

  }

  VelocityForgeClientConnectionPhase nextPhase() {
    return this;
  }

  @Override
  public boolean consideredComplete() {
    return false;
  }

  public clientResetType getResetType() {
    return resetType;
  }
  enum clientResetType {
    UNKNOWN,
    NONE,
    CRP {
      @Override
      void doReset(ConnectedPlayer player) {
        MinecraftConnection connection = player.getConnection();

        //There is no going back even if the handshake fails. No reason to still be connected.
        if (player.getConnectedServer() != null) {
          player.getConnectedServer().disconnect();
          player.setConnectedServer(null);
        }
        //Don't handle anything from the server until the reset has completed.
        if (player.getConnectionInFlight() != null) {
          player.getConnectionInFlight().getConnection().getChannel().config().setAutoRead(false);
        }

        if (connection.getState() == StateRegistry.PLAY || connection.getState() == StateRegistry.CONFIG) {
          connection.write(new PluginMessage("fml:handshake", Unpooled.wrappedBuffer(ForgeHandshakeUtils.generatePluginResetPacket())));
          connection.setState(StateRegistry.LOGIN);
        } else {
          connection.write(new LoginPluginMessagePacket(98,"fml:loginwrapper", Unpooled.wrappedBuffer(ForgeHandshakeUtils.generateResetPacket())));
        }

        //Prepare to receive reset ACK
        connection.getChannel().pipeline().addBefore(Connections.MINECRAFT_DECODER,
                ForgeConstants.RESET_LISTENER, new FML2CRPMResetCompleteDecoder());

        //Transition
        player.setPhase(WAITING_RESET);
        WAITING_RESET.onTransitionToNewPhase(player);
      }
    },
    SR {
      @Override
      void doReset(ConnectedPlayer player) {
        ByteBuf buf = Unpooled.buffer();
        ProtocolUtils.writeVarInt(buf, 0);
        buf.writeBytes((player.getVirtualHost().get().getHostName() + ":"
                + player.getVirtualHost().get().getPort()).getBytes(StandardCharsets.UTF_8));
        player.getConnection().write(new PluginMessage("srvredirect:red", buf));
      }
    };

    void doReset(ConnectedPlayer player) {
    }
  }
}
