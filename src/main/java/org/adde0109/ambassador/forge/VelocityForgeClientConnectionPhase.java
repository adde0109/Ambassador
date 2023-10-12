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
import com.velocitypowered.proxy.protocol.packet.LoginPluginMessage;
import com.velocitypowered.proxy.protocol.packet.PluginMessage;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.kyori.adventure.text.Component;
import org.adde0109.ambassador.Ambassador;
import org.adde0109.ambassador.forge.packet.IForgeLoginWrapperPacket;
import org.adde0109.ambassador.forge.packet.ModListReplyPacket;
import org.adde0109.ambassador.forge.pipeline.ForgeLoginWrapperDecoder;
import org.adde0109.ambassador.velocity.client.FML2CRPMResetCompleteDecoder;
import org.adde0109.ambassador.velocity.client.OutboundSuccessHolder;
import org.adde0109.ambassador.velocity.client.ClientPacketQueue;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

public enum VelocityForgeClientConnectionPhase implements ClientConnectionPhase {

  NOT_STARTED {
    @Override
    VelocityForgeClientConnectionPhase nextPhase() {
      return IN_PROGRESS;
    }

    @Override
    public void resetConnectionPhase(ConnectedPlayer player) {
      RESETTABLE.resetConnectionPhase(player);
    }
  },
  IN_PROGRESS {
  },
  RESETTABLE {
    @Override
    void onTransitionToNewPhase(ConnectedPlayer player) {
      //Plugins may now send packets to client
      player.getConnection().getChannel().pipeline().remove(ForgeConstants.PLUGIN_PACKET_QUEUE);
      ((VelocityServer) Ambassador.getInstance().server).registerConnection(player);
    }

    @Override
    public void resetConnectionPhase(ConnectedPlayer player) {
      MinecraftConnection connection = player.getConnection();

      //There is no going back even if the handshake fails. No reason to still be connected.
      if (player.getConnectedServer() != null) {
        player.getConnectedServer().disconnect();
        player.setConnectedServer(null);
      }
      //Don't handle anything from the server until the reset has completed.
      //player.getConnectionInFlight().getConnection().getChannel().config().setAutoRead(false);

      if (connection.getState() == StateRegistry.PLAY || connection.getState() == StateRegistry.CONFIG) {
        connection.write(new PluginMessage("fml:handshake", Unpooled.wrappedBuffer(ForgeHandshakeUtils.generatePluginResetPacket())));
        connection.setState(StateRegistry.LOGIN);
      } else {
        connection.write(new LoginPluginMessage(98,"fml:loginwrapper", Unpooled.wrappedBuffer(ForgeHandshakeUtils.generateResetPacket())));
      }

      //Prepare to receive reset ACK
      connection.getChannel().pipeline().addBefore(Connections.MINECRAFT_DECODER,
              ForgeConstants.RESET_LISTENER, new FML2CRPMResetCompleteDecoder());

      //Transition
      player.setPhase(WAITING_RESET);
      WAITING_RESET.onTransitionToNewPhase(player);
    }

    @Override
    public boolean consideredComplete() {
      return true;
    }
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
      if (msg.getId() == 98) {
        player.getConnection().getChannel().pipeline().remove(ForgeConstants.RESET_LISTENER);
        player.setPhase(NOT_STARTED);

        player.getConnection().getChannel().pipeline().remove(ForgeConstants.LOGIN_PACKET_QUEUE);

        if (!(server.getConnection().getType() instanceof ForgeFMLConnectionType)) {
          // -> vanilla
          complete(player, msg.getSuccess());
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
      Ambassador.getTemporaryForced().put(player.getUsername(), player.getConnectionInFlight().getServer(),
              Ambassador.getInstance().config.getServerSwitchCancellationTime(), TimeUnit.SECONDS);
      //Disconnect - Reset
      if(player.getModInfo().isPresent()
              && player.getModInfo().get().getMods().stream().anyMatch((mod -> mod.getId().equals("serverredirect")
              || mod.getId().equals("srvredirect:red")))
              && player.getVirtualHost().isPresent()) {
        ByteBuf buf = Unpooled.buffer();
        ProtocolUtils.writeVarInt(buf, 0);
        buf.writeBytes((player.getVirtualHost().get().getHostName() + ":"
                + player.getVirtualHost().get().getPort()).getBytes(StandardCharsets.UTF_8));
        player.getConnection().write(new PluginMessage("srvredirect:red", buf));
      } else {
        player.disconnect(Ambassador.getInstance().config.getDisconnectResetMessage());
      }
    }

    @Override
    public boolean consideredComplete() {
      return true;
    }
  };

  public ModListReplyPacket clientModList;

  public boolean handle(ConnectedPlayer player, IForgeLoginWrapperPacket msg, VelocityServerConnection server) {
    player.setPhase(nextPhase());

    if (msg instanceof ModListReplyPacket replyPacket) {
      ModInfo modInfo = new ModInfo("FML2", replyPacket.getMods().stream().map(
              (v) -> new ModInfo.Mod(v,"1")).toList());
      player.setModInfo(modInfo);
      this.clientModList = replyPacket;
      if (!(server.getConnection().getType() instanceof ForgeFMLConnectionType)) {
        complete(player);
        player.getConnectionInFlight().getConnection().getChannel().config().setAutoRead(true);
        return true;
      }
      replyPacket.getChannels().put(MinecraftChannelIdentifier.from("ambassador:commands"),"1");
    }

    player.getConnectionInFlight().getConnection().write(msg.encode());
    return true;
  }

  public void sendVanillaModlist(ConnectedPlayer player) {
    player.getConnection().write(new LoginPluginMessage(0, "fml:loginwrapper",
            Unpooled.wrappedBuffer(player.getConnection().getType() == ForgeConstants.ForgeFML3 ?
                    ForgeHandshakeUtils.emptyModlistFML3 : ForgeHandshakeUtils.emptyModlistFML2)));

    ForgeLoginWrapperDecoder decoder = (ForgeLoginWrapperDecoder) player.getConnection()
            .getChannel().pipeline().get(ForgeConstants.FORGE_HANDSHAKE_DECODER);
    decoder.registerLoginWrapperID(0);
  }

  public void complete(ConnectedPlayer player) {
    complete(player, isResettable(player));
  }

  public void complete(ConnectedPlayer player, boolean resettable) {
    MinecraftConnection connection = player.getConnection();
    ((OutboundSuccessHolder) connection.getChannel().pipeline().get(ForgeConstants.SERVER_SUCCESS_LISTENER))
            .sendPacket();
    connection.setState(StateRegistry.PLAY);

    if (resettable) {
      player.setPhase(RESETTABLE);
      RESETTABLE.onTransitionToNewPhase(player);
      RESETTABLE.clientModList = clientModList;
    } else {
      player.setPhase(COMPLETE);
      COMPLETE.onTransitionToNewPhase(player);
      COMPLETE.clientModList = clientModList;
    }
  }

  private boolean isResettable(ConnectedPlayer player) {
    if (player.getModInfo().isPresent()) {
      return player.getModInfo().get().getMods().stream().anyMatch((mod -> mod.getId().equals("clientresetpacket")));
    }
    return false;
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


}
