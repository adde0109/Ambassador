package org.adde0109.ambassador.velocity;

import com.velocitypowered.api.event.player.KickedFromServerEvent;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.connection.MinecraftConnection;
import com.velocitypowered.proxy.connection.backend.VelocityServerConnection;
import com.velocitypowered.proxy.connection.client.ClientConnectionPhase;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import com.velocitypowered.proxy.network.Connections;
import com.velocitypowered.proxy.protocol.StateRegistry;
import com.velocitypowered.proxy.protocol.packet.LoginPluginMessage;
import com.velocitypowered.proxy.protocol.packet.LoginPluginResponse;
import com.velocitypowered.proxy.protocol.packet.PluginMessage;
import io.netty.buffer.Unpooled;
import net.kyori.adventure.text.Component;
import org.adde0109.ambassador.Ambassador;
import org.adde0109.ambassador.forge.FML2CRPMResetCompleteDecoder;
import org.adde0109.ambassador.forge.ForgeConstants;
import org.adde0109.ambassador.forge.ForgeHandshakeUtils;
import org.adde0109.ambassador.velocity.client.OutboundForgeHandshakeHolder;

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
      player.getConnectedServer().getConnection().getChannel().config().setAutoRead(false);

      connection.getChannel().pipeline().addBefore(Connections.MINECRAFT_DECODER, ForgeConstants.RESET_LISTENER,new FML2CRPMResetCompleteDecoder());
      connection.getChannel().pipeline().addLast(ForgeConstants.FORGE_HANDSHAKE_HOLDER,new OutboundForgeHandshakeHolder());

      player.getConnection().setSessionHandler(new VelocityForgeHandshakeSessionHandler(player.getConnection().getSessionHandler(),player));

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
        player.disconnect(Component.text(Ambassador.getInstance().config.getDisconnectResetMessage()));
      }, Ambassador.getInstance().config.getResetTimeout(), TimeUnit.MILLISECONDS);
    }
    @Override
    public boolean handle(ConnectedPlayer player, LoginPluginResponse response, VelocityServerConnection server) {
      if (response.getId() == 98) {
        if (scheduledFuture.cancel(false)) {
          player.getConnection().getChannel().pipeline().remove(ForgeConstants.RESET_LISTENER);
          player.getConnection().setState(StateRegistry.LOGIN);
          player.setPhase(NOT_STARTED);
          //Send all held messages
          player.getConnection().getChannel().pipeline().remove(ForgeConstants.FORGE_HANDSHAKE_HOLDER);
        }
        return true;
      } else {
        return false;
      }
    }
  };




  public ServerConnection internalServerConnection;

  public boolean handle(ConnectedPlayer player, LoginPluginResponse response, VelocityServerConnection server) {
    player.setPhase(nextPhase());

    player.getConnectionInFlight().getConnection().write(response.retain());
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



/*
  public void handleKick(KickedFromServerEvent event) {
    //If kicked before the client has entered PLAY and has been reset.
    if (lastKnownWorking != null && !(event.getResult() instanceof KickedFromServerEvent.RedirectPlayer)) {
      net.kyori.adventure.text.Component reason = event.getServerKickReason().orElse(null);
      event.setResult(KickedFromServerEvent.RedirectPlayer.create(lastKnownWorking,reason));
      lastKnownWorking = null;
    }
  }
 */
}
