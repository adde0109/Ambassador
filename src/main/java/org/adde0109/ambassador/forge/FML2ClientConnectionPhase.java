package org.adde0109.ambassador.forge;

public class FML2ClientConnectionPhase {

/*
  @Override
  public RegisteredServer chooseServer(ConnectedPlayer player) {
    forced = Ambassador.getTemporaryForced().remove(player.getUsername());
    return forced;
  }

  @Override
  public CompletableFuture<Boolean> reset(RegisteredServer server, ConnectedPlayer player) {
    FML2CRPMClientConnectionPhase newPhase = new FML2CRPMClientConnectionPhase(clientPhase,getPayloadManager());
    player.setPhase(newPhase);
    CompletableFuture<Boolean> future = newPhase.reset(server,player);
    future.thenAccept(success -> {
      if (!success) {
        Ambassador.getTemporaryForced().put(player.getUsername(),server, Ambassador.getInstance().config.getServerSwitchCancellationTime(), TimeUnit.SECONDS);
        player.disconnect(Component.text(Ambassador.getInstance().config.getDisconnectResetMessage()));
      }
    });
    return future;
  }



/*
  public void handleForward(VelocityServerConnection serverConnection, LoginPluginMessage payload) {
    final ByteBuf buf = payload.content().duplicate();
    ProtocolUtils.readString(buf);  //Channel
    ProtocolUtils.readVarInt(buf);  //Length
    if (ProtocolUtils.readVarInt(buf) == 1) {
      getPayloadManager().listenFor(payload.getId()).thenAccept(rawResponse -> {
        final ByteBuf response = rawResponse.duplicate();
        ProtocolUtils.readString(response);  //Channel
        ProtocolUtils.readVarInt(response);  //Length
        if (ProtocolUtils.readVarInt(response) == 2) {
          String[] mods = ProtocolUtils.readStringArray(response);
          if (Arrays.stream(mods).anyMatch(s -> s.equals("clientresetpacket"))) {
            serverConnection.getPlayer().setPhase(new FML2CRPMClientConnectionPhase(clientPhase,getPayloadManager()));
          }
        }
      });
    }

  }
 */
}
