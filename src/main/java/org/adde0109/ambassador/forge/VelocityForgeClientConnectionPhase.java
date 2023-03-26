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
import com.velocitypowered.proxy.protocol.packet.PluginMessage;
import io.netty.buffer.Unpooled;
import org.adde0109.ambassador.Ambassador;
import org.adde0109.ambassador.forge.packet.IForgeLoginWrapperPacket;
import org.adde0109.ambassador.forge.packet.ModListReplyPacket;
import org.adde0109.ambassador.velocity.client.FML2CRPMResetCompleteDecoder;
import org.adde0109.ambassador.velocity.client.OutboundForgeHandshakeHolder;
import org.adde0109.ambassador.velocity.client.OutboundSuccessHolder;

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
    public void resetConnectionPhase(ConnectedPlayer player) {
      MinecraftConnection connection = player.getConnection();


      //We unregister so no plugin sees this client while the client is being reset.
      ((VelocityServer) Ambassador.getInstance().server).unregisterConnection(player);
      if (player.getConnectedServer() != null) {
        player.getConnectedServer().disconnect();
        player.setConnectedServer(null);
      }
      player.getConnectionInFlight().getConnection().getChannel().config().setAutoRead(false);

      connection.getChannel().pipeline().addBefore(Connections.MINECRAFT_DECODER, ForgeConstants.RESET_LISTENER,new FML2CRPMResetCompleteDecoder());
      connection.getChannel().pipeline().addAfter(Connections.MINECRAFT_ENCODER, ForgeConstants.FORGE_HANDSHAKE_HOLDER,new OutboundForgeHandshakeHolder());


      connection.write(new PluginMessage("fml:handshake", Unpooled.wrappedBuffer(ForgeHandshakeUtils.generatePluginResetPacket())));

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
          player.getConnection().getChannel().pipeline().remove(ForgeConstants.FORGE_HANDSHAKE_HOLDER);
          player.getConnectionInFlight().getConnection().getChannel().config().setAutoRead(true);

          if (!(server.getConnection().getType() instanceof ForgeFMLConnectionType)) {
            MinecraftConnection connection = player.getConnection();
            ((OutboundSuccessHolder) connection.getChannel().pipeline().get(ForgeConstants.SERVER_SUCCESS_LISTENER))
                    .sendPacket();
            connection.setState(StateRegistry.PLAY);
          }
        }
        return true;
      } else {
        return false;
      }
    }
  };




  public boolean vanillaMode = true;

  public boolean handle(ConnectedPlayer player, IForgeLoginWrapperPacket msg, VelocityServerConnection server) {
    player.setPhase(nextPhase());

    if (msg instanceof ModListReplyPacket replyPacket) {
      replyPacket.getChannels().put(MinecraftChannelIdentifier.from("ambassador:commands"),"1");
    }

    player.getConnectionInFlight().getConnection().write(msg.encode());
    vanillaMode = false;
    return true;
  }

  private RegisteredServer lastKnownWorking;

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
