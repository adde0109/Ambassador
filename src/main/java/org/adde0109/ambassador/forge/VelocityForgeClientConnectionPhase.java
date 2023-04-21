package org.adde0109.ambassador.forge;

import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.connection.MinecraftConnection;
import com.velocitypowered.proxy.connection.backend.VelocityServerConnection;
import com.velocitypowered.proxy.connection.client.ClientConnectionPhase;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import com.velocitypowered.proxy.network.Connections;
import com.velocitypowered.proxy.protocol.StateRegistry;
import com.velocitypowered.proxy.protocol.packet.LoginPluginMessage;
import com.velocitypowered.proxy.protocol.packet.PluginMessage;
import io.netty.buffer.Unpooled;
import org.adde0109.ambassador.Ambassador;
import org.adde0109.ambassador.forge.packet.IForgeLoginWrapperPacket;
import org.adde0109.ambassador.forge.packet.ModListReplyPacket;
import org.adde0109.ambassador.forge.pipeline.ForgeLoginWrapperDecoder;
import org.adde0109.ambassador.velocity.client.FML2CRPMResetCompleteDecoder;
import org.adde0109.ambassador.velocity.client.OutboundSuccessHolder;
import org.adde0109.ambassador.velocity.client.PluginLoginPacketQueue;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.ScheduledFuture;

public enum VelocityForgeClientConnectionPhase implements ClientConnectionPhase {
  //TODO:Make class when PCF is done

  NOT_STARTED {
    @Override
    VelocityForgeClientConnectionPhase nextPhase() {
      return IN_PROGRESS;
    }
  },
  IN_PROGRESS {

  },
  COMPLETE {

    @Override
    public void resetAndWrite(ConnectedPlayer player, LoginPluginMessage message) {
      resetConnectionPhase(player);
      pending = message;
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
      player.getConnectionInFlight().getConnection().getChannel().config().setAutoRead(false);

      //Prepare to receive reset ACK
      connection.getChannel().pipeline().addBefore(Connections.MINECRAFT_DECODER, ForgeConstants.RESET_LISTENER,new FML2CRPMResetCompleteDecoder());
      ((ForgeLoginWrapperDecoder) connection.getChannel().pipeline().get(ForgeConstants.FORGE_HANDSHAKE_DECODER)).registerLoginWrapperID(98);

      //No more PLAY packets past this point should be sent to the client in case the reset works.
      connection.write(new PluginMessage("fml:handshake", Unpooled.wrappedBuffer(ForgeHandshakeUtils.generatePluginResetPacket())));

      //We unregister so no plugin sees this client while the client is being reset.
      ((VelocityServer) Ambassador.getInstance().server).unregisterConnection(player);
      connection.getChannel().pipeline().addAfter(Connections.MINECRAFT_ENCODER,ForgeConstants.PLUGIN_PACKET_QUEUE, new PluginLoginPacketQueue());

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

    ScheduledFuture<?> scheduledFuture;
    @Override
    void onTransitionToNewPhase(ConnectedPlayer player) {
      scheduledFuture = player.getConnection().eventLoop().schedule(()-> {
        player.getConnection().getChannel().pipeline().remove(ForgeConstants.RESET_LISTENER);
        Ambassador.getTemporaryForced().put(player.getUsername(), player.getConnectionInFlight().getServer(),
                Ambassador.getInstance().config.getServerSwitchCancellationTime(), TimeUnit.SECONDS);
        //Disconnect - Reset Timeout
        player.disconnect(Ambassador.getInstance().config.getDisconnectResetMessage());
      }, Ambassador.getInstance().config.getResetTimeout(), TimeUnit.MILLISECONDS);
    }
    @Override
    public boolean handle(ConnectedPlayer player, IForgeLoginWrapperPacket msg, VelocityServerConnection server) {
      if (msg.getId() == 98) {
        if (scheduledFuture.cancel(false)) {
          player.getConnection().getChannel().pipeline().remove(ForgeConstants.RESET_LISTENER);
          player.getConnection().setState(StateRegistry.LOGIN);
          player.setPhase(NOT_STARTED);

          //Send all held messages
          if (pending != null)
            player.getConnection().write(pending);
          player.getConnectionInFlight().getConnection().getChannel().config().setAutoRead(true);

          if (!(server.getConnection().getType() instanceof ForgeFMLConnectionType)) {
            //Forge -> Vanilla
            MinecraftConnection connection = player.getConnection();
            ((OutboundSuccessHolder) connection.getChannel().pipeline().get(ForgeConstants.SERVER_SUCCESS_LISTENER))
                    .sendPacket();
            connection.setState(StateRegistry.PLAY);

            //Plugins may now send packets to client
            connection.getChannel().pipeline().remove(ForgeConstants.PLUGIN_PACKET_QUEUE);
            ((VelocityServer) Ambassador.getInstance().server).registerConnection(player);
          }
        }
        return true;
      } else {
        return false;
      }
    }
  };
  public LoginPluginMessage pending;

  public boolean handle(ConnectedPlayer player, IForgeLoginWrapperPacket msg, VelocityServerConnection server) {
    player.setPhase(nextPhase());

    if (msg instanceof ModListReplyPacket replyPacket) {
      replyPacket.getChannels().put(MinecraftChannelIdentifier.from("ambassador:commands"),"1");
    }

    player.getConnectionInFlight().getConnection().write(msg.encode());
    return true;
  }

  void onTransitionToNewPhase(ConnectedPlayer player) {

  }

  public void resetAndWrite(ConnectedPlayer player, LoginPluginMessage message) {
    player.getConnection().write(message);
  }

  VelocityForgeClientConnectionPhase nextPhase() {
    return this;
  }

  @Override
  public boolean consideredComplete() {
    return false;
  }


}
