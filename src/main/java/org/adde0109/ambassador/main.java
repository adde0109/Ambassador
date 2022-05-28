package org.adde0109.ambassador;

import com.google.common.io.ByteArrayDataInput;
import com.google.inject.Inject;
import com.velocitypowered.api.event.Continuation;
import com.velocitypowered.api.event.EventTask;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PreLoginEvent;
import com.velocitypowered.api.event.player.ServerLoginPluginMessageEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.LoginPhaseConnection;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.messages.ChannelIdentifier;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerPing;
import com.velocitypowered.api.util.ModInfo;
import java.io.EOFException;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.units.qual.A;
import org.slf4j.Logger;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Stream;

@Plugin(id = "ambassador", name = "Ambassador", version = "0.1.0-SNAPSHOT", url = "", description = "", authors = {"adde0109"})
public class main {

  private final ProxyServer server;
  private final Logger logger;
  private final RegisteredServer forgeServer;

  private LoginPhaseConnection inbound;
  private ChannelIdentifier loginWrapperChannel;

  private static final int MAX_DATA_LENGTH = 16000;
  private static final int PACKET_LENGTH_INDEX = 14;

  @Inject
  public main(ProxyServer server, Logger logger) {
    this.server = server;
    this.logger = logger;
    forgeServer = server.getServer("lobby").orElseThrow(IllegalAccessError::new);

    loginWrapperChannel = MinecraftChannelIdentifier.create("fml","loginwrapper");
  }

  private int numberOfRecivedParts;
  private byte[] recivedParts;
  private int recivedBytes;
  private int head;

  private byte[] recivedClientModlist;
  private byte[] recivedClientACK;



  @Subscribe
  public void onPreLoginEvent(PreLoginEvent event, Continuation continuation) {
    inbound = (LoginPhaseConnection) event.getConnection();

    recivedParts = new byte[1000000000];
    recivedBytes = 0;
    numberOfRecivedParts = 0;
    ping(continuation);

  }

  private void ping(Continuation continuation) {
    forgeServer.ping().thenAccept((s) -> onBackendPong(s,continuation));
  }


  public void onBackendPong(ServerPing status, Continuation continuation) {
    numberOfRecivedParts++;

    ModInfo.Mod pair = status.getModinfo().orElseThrow(IllegalAccessError::new).getMods().get(0);

    int[] values = Arrays.stream(pair.getVersion().substring(pair.getVersion().indexOf(":")+1).split(":")).map(Integer::parseInt).mapToInt(x -> x).toArray();

    int parts = Integer.parseInt((pair.getVersion().split(":")[0].split("-"))[1]);
    int recivedPartNr = Integer.parseInt((pair.getVersion().split(":")[0].split("-"))[0]);

    logger.warn("recived part " + String.valueOf(numberOfRecivedParts) + " out of " + String.valueOf(parts));


    byte[] temp = pair.getId().getBytes(StandardCharsets.ISO_8859_1);
    head = (recivedPartNr-1)*MAX_DATA_LENGTH;
    for(int i = 0;i<temp.length;i++) {
      recivedParts[head] = temp[i];
      head++;
      recivedBytes++;
    }


    logger.warn("test");
    if(numberOfRecivedParts >= parts)
    {
      sendHandshake(splitPacket(recivedParts,values));
      continuation.resume();
    }
    else {
      ping(continuation);
      logger.warn("Pinged!");
    }
  }
  private List<byte[]> splitPacket(byte[] data, int[] startPacketMarkers) {
    List<byte[]> list = new ArrayList<>();
    for(int i = 0;i<startPacketMarkers.length-1;i++) {
      list.add(getPacket(data, startPacketMarkers[i],startPacketMarkers[i+1]-1));
    }
    list.add(getPacket(data,startPacketMarkers[startPacketMarkers.length-1],recivedBytes-1));
    return list;
  }

  private byte[] getPacket(byte[] data, int startByteIndex,int lastByteIndex) {
    byte[] temp = new byte[lastByteIndex-startByteIndex+1];

    for(int i = startByteIndex; i<=lastByteIndex;i++) {
      temp[i-startByteIndex] = data[i];
    }
    return temp;
  }

  private void sendHandshake(List<byte[]> handshakePackets) {
    handshakePackets.forEach((packet) -> {
      inbound.sendLoginPluginMessage(loginWrapperChannel, packet, new LoginPhaseConnection.MessageConsumer() {
        @Override
        public void onMessageResponse(byte @Nullable [] responseBody) {
          if (responseBody.length < PACKET_LENGTH_INDEX+5+5) {
            logger.warn("ACK recived!");
            recivedClientACK = responseBody;
          }
          else {
            logger.warn("Modlist recived!");
            recivedClientModlist = responseBody;
          }
        }
      });
    });
  }

  @Subscribe
  public void onServerLoginPluginMessageEvent(ServerLoginPluginMessageEvent event, Continuation continuation) {
    ByteArrayDataInput data = event.contentsAsDataStream();
    if(data.skipBytes(PACKET_LENGTH_INDEX) != PACKET_LENGTH_INDEX)  //Channel Identifier
      continuation.resumeWithException(new EOFException());
    readVarInt(data); //Length
    int packetID = readVarInt(data);

    if(packetID == 1) {
      event.setResult(ServerLoginPluginMessageEvent.ResponseResult.reply(recivedClientModlist));
    }
    else {
      event.setResult(ServerLoginPluginMessageEvent.ResponseResult.reply(recivedClientACK));
    }
    continuation.resume();

  }


  public static int readVarInt(ByteArrayDataInput stream) {
    int i = 0;
    int j = 0;

    byte b0;
    do {
      b0 = stream.readByte();
      i |= (b0 & 127) << j++ * 7;
      if (j > 5) {
        throw new RuntimeException("VarInt too big");
      }
    } while((b0 & 128) == 128);

    return i;
  }


}
