package org.adde0109.ambassador;

import com.velocitypowered.api.event.Continuation;
import com.velocitypowered.api.event.connection.PreLoginEvent;
import com.velocitypowered.api.network.ProtocolVersion;

public class ForgeConnectionHandler {



  public void onPreLogin(PreLoginEvent event, Continuation continuation) {
    if(event.getConnection().getProtocolVersion() == ProtocolVersion.MINECRAFT_1_16_4) {

    }
    else {
      continuation.resume();
    }
  }

}
