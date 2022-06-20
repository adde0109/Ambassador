package org.adde0109.ambassador;

import com.google.common.io.ByteArrayDataInput;
import com.velocitypowered.api.event.Continuation;
import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PreLoginEvent;
import com.velocitypowered.api.event.permission.PermissionsSetupEvent;
import com.velocitypowered.api.event.player.ServerLoginPluginMessageEvent;
import com.velocitypowered.api.proxy.InboundConnection;
import com.velocitypowered.api.proxy.LoginPhaseConnection;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerPing;
import com.velocitypowered.api.util.ModInfo;
import java.io.EOFException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

public class ForgeHandshakeDataHandler {

  public Map<RegisteredServer, handshakeReceiver.CachedServerHandshake> cachedServerHandshake = new HashMap<RegisteredServer, handshakeReceiver.CachedServerHandshake>();
  public byte[] recivedClientACK;
  public Map<RegisteredServer,byte[]> recivedClientModlist = new HashMap<RegisteredServer,byte[]>();
  public LoginPhaseConnection connection;

  private Collection<InboundConnection> pendingConnections = new HashSet<InboundConnection>();

  private static final int PACKET_LENGTH_INDEX = 14;    //length of "fml:handshake"+1

  private final AmbassadorConfig config;
  private final Logger logger;

  public ForgeHandshakeDataHandler(AmbassadorConfig config,Logger logger) {
    this.config = config;
    this.logger = logger;
  }

  @Subscribe
  public void onPreLoginEvent(PreLoginEvent event, Continuation continuation) {
    if (!config.shouldHandle(event.getConnection().getProtocolVersion().getProtocol())) {
      continuation.resume();
      return;
    }
    RegisteredServer server = config.getServer(event.getConnection().getProtocolVersion().getProtocol());
    getHandshake(server).thenAccept(p -> {
      sendModlist(p.modListPacket,(LoginPhaseConnection) event.getConnection(),server);
      sendOther(p.otherPackets,(LoginPhaseConnection) event.getConnection());
      continuation.resume();
    });
  }

  @Subscribe
  public void onServerLoginPluginMessageEvent(ServerLoginPluginMessageEvent event, Continuation continuation) {
    if((!recivedClientModlist.containsKey(event.getConnection().getServer())) || (recivedClientACK == null)) {
      continuation.resume();
      return;
    }
    ByteArrayDataInput data = event.contentsAsDataStream();
    if(data.skipBytes(PACKET_LENGTH_INDEX) != PACKET_LENGTH_INDEX) {  //Channel Identifier
      continuation.resumeWithException(new EOFException());
      return;
    }
    readVarInt(data); //Length
    int packetID = readVarInt(data);

    if(packetID == 1) {
      event.setResult(ServerLoginPluginMessageEvent.ResponseResult.reply(recivedClientModlist.get(event.getConnection().getServer())));
    }
    else {
      event.setResult(ServerLoginPluginMessageEvent.ResponseResult.reply(recivedClientACK));
    }
    continuation.resume();

  }


  public CompletableFuture<handshakeReceiver.CachedServerHandshake> getHandshake(RegisteredServer handshakeServer) {
    CompletableFuture<handshakeReceiver.CachedServerHandshake> future;
    if((handshakeServer.getPlayersConnected().isEmpty()) || (!cachedServerHandshake.containsKey(handshakeServer))) {
      handshakeReceiver receiver = new handshakeReceiver(handshakeServer, logger);
      future = receiver.downloadHandshake();
      future.thenAccept(p -> {
        cachedServerHandshake.put(handshakeServer,p);
      });
      return future;
    }
    else {
      future = new CompletableFuture<>();
      future.complete(cachedServerHandshake.get(handshakeServer));
      return future;
    }
  }

  private void sendModlist(byte[] modListPacket, LoginPhaseConnection connection, RegisteredServer server) {
      connection.sendLoginPluginMessage(MinecraftChannelIdentifier.create("fml","loginwrapper"), modListPacket, responseBody ->  recivedClientModlist.put(server,responseBody));
  }

  private void sendOther(List<byte[]> otherPackets, LoginPhaseConnection connection) {
    for (int i = 0;i<otherPackets.size();i++) {
      connection.sendLoginPluginMessage(MinecraftChannelIdentifier.create("fml","loginwrapper"), otherPackets.get(i),
              (i<(otherPackets.size()-1)) ? responseBody -> recivedClientACK = responseBody : responseBody -> {
        pendingConnections.add(connection);
              });
    }
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

  private class handshakeReceiver {

    private int partLength;

    private final Logger logger;

    private int numberOfRecivedParts;
    private byte[] recivedParts;
    private int recivedBytes;
    private final RegisteredServer forgeServer;

    private handshakeReceiver(RegisteredServer server, Logger logger) {

      this.logger = logger;
      this.forgeServer = server;

    }

    public CompletableFuture<CachedServerHandshake> downloadHandshake() {
      CompletableFuture<CachedServerHandshake> future = new CompletableFuture<CachedServerHandshake>();
      ping(future);
      return future;
    }

    private void ping(CompletableFuture<CachedServerHandshake> future) {
      forgeServer.ping().thenAccept((s) -> onBackendPong(s, future));
    }

    public void onBackendPong(ServerPing status, CompletableFuture<CachedServerHandshake> future) {
      numberOfRecivedParts++;
      if ((!status.getModinfo().isPresent()) || (!Objects.equals(status.getModinfo().get().getType(), "ambassador"))) {
        future.cancel(true);
        logger.error("The specified Forge server is not running the Forge-side version of this plugin!");
        return;
      }


      ModInfo.Mod pair = status.getModinfo().orElseThrow(IllegalAccessError::new).getMods().get(0);

      int[] values = Arrays.stream(pair.getVersion().substring(pair.getVersion().indexOf(":") + 1).split(":")).map(Integer::parseInt).mapToInt(x -> x).toArray();

      int totalLength = Integer.parseInt((pair.getVersion().split(":")[0].split("-"))[2]);
      int parts = Integer.parseInt((pair.getVersion().split(":")[0].split("-"))[1]);
      int recivedPartNr = Integer.parseInt((pair.getVersion().split(":")[0].split("-"))[0]);

      logger.info("Downloaded part " + String.valueOf(numberOfRecivedParts) + " out of " + String.valueOf(parts));

      if(recivedParts == null) {
        recivedParts = new byte[totalLength];
        partLength = pair.getId().getBytes(StandardCharsets.ISO_8859_1).length;
      }

      placePartInArray(pair.getId().getBytes(StandardCharsets.ISO_8859_1), recivedPartNr - 1);

      if (numberOfRecivedParts >= parts) {
        List<byte[]> packets = splitPackets(recivedParts,values);
        future.complete(new CachedServerHandshake("",packets.get(0),packets.subList(1,packets.size()-1)));
      } else {
        ping(future);
      }
    }



    private void placePartInArray(byte[] temp, int partNr) {
      int head = partNr * partLength;
      for (int i = 0; i < temp.length; i++) {
        recivedParts[head] = temp[i];
        head++;
        recivedBytes++;
      }
    }

    private byte[] getPacket(byte[] data, int startByteIndex, int lastByteIndex) {
      byte[] temp = new byte[lastByteIndex - startByteIndex + 1];

      for (int i = startByteIndex; i <= lastByteIndex; i++) {
        temp[i - startByteIndex] = data[i];
      }
      return temp;
    }


    private List<byte[]> splitPackets(byte[] data, int[] startPacketMarkers) {
      List<byte[]> list = new ArrayList<>();
      for (int i = 0; i < startPacketMarkers.length - 1; i++) {
        list.add(getPacket(data, startPacketMarkers[i], startPacketMarkers[i + 1] - 1));
      }
      list.add(getPacket(data, startPacketMarkers[startPacketMarkers.length - 1], recivedBytes - 1));
      return list;
    }


    private class CachedServerHandshake {
      private String sessionID;
      private byte[] modListPacket;
      private List<byte[]> otherPackets;

      private CachedServerHandshake(String sessionID,byte[] modListPacket,List<byte[]> otherPackets) {
        this.sessionID = sessionID;
        this.modListPacket = modListPacket;
        this.otherPackets = otherPackets;
      }
    }
  }
}
