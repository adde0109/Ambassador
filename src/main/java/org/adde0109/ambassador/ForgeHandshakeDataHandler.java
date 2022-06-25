package org.adde0109.ambassador;

import com.google.common.io.ByteArrayDataInput;
import com.velocitypowered.api.event.Continuation;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PreLoginEvent;
import com.velocitypowered.api.event.player.ServerLoginPluginMessageEvent;
import com.velocitypowered.api.event.player.ServerPostConnectEvent;
import com.velocitypowered.api.proxy.ConnectionRequestBuilder;
import com.velocitypowered.api.proxy.InboundConnection;
import com.velocitypowered.api.proxy.LoginPhaseConnection;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerPing;
import com.velocitypowered.api.util.ModInfo;
import java.io.EOFException;
import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import net.kyori.adventure.text.Component;
import org.slf4j.Logger;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

public class ForgeHandshakeDataHandler {

  public Map<RegisteredServer,CachedServerHandshake> cachedServerHandshake = new HashMap<RegisteredServer,CachedServerHandshake>();
  public byte[] recivedClientACK;
  public Map<RegisteredServer,byte[]> recivedClientModlist = new HashMap<RegisteredServer,byte[]>();
  public LoginPhaseConnection connection;


  private Map<InboundConnection,RegisteredServer> syncedConnections  = new HashMap<InboundConnection,RegisteredServer>();

  private static final int PACKET_LENGTH_INDEX = 14;    //length of "fml:handshake"+1

  private final Logger logger;
  private final ProxyServer server;

  public ForgeHandshakeDataHandler(Logger logger, ProxyServer server) {
    this.logger = logger;
    this.server = server;
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

  public static class handshakeReceiver {

    private int partLength;

    private final Logger logger;

    private int numberOfRecivedParts;
    private byte[] recivedParts;
    private int recivedBytes;
    private final RegisteredServer forgeServer;

    public handshakeReceiver(RegisteredServer server, Logger logger) {

      this.logger = logger;
      this.forgeServer = server;

    }

    public CompletableFuture<CachedServerHandshake> downloadHandshake() {
      CompletableFuture<CachedServerHandshake> future = new CompletableFuture<CachedServerHandshake>();
      ping(future);
      return future;
    }

    private void ping(CompletableFuture<CachedServerHandshake> future) {
      forgeServer.ping().whenComplete((msg,ex) -> {
        if (ex != null) {
          future.completeExceptionally(ex);
        } else {
          onBackendPong(msg, future);
        }
      });
    }

    public void onBackendPong(ServerPing status, CompletableFuture<CachedServerHandshake> future) {
      numberOfRecivedParts++;
      if ((status.getModinfo().isEmpty()) || (!Objects.equals(status.getModinfo().get().getType(), "ambassador"))) {
        future.completeExceptionally(new Exception("The specified Forge server is not running the Forge-side version of this plugin!"));
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
      for (byte b : temp) {
        recivedParts[head] = b;
        head++;
        recivedBytes++;
      }
    }

    private byte[] getPacket(byte[] data, int startByteIndex, int lastByteIndex) {
      byte[] temp = new byte[lastByteIndex - startByteIndex + 1];

      if (lastByteIndex + 1 - startByteIndex >= 0)
        System.arraycopy(data, startByteIndex, temp, 0,
            lastByteIndex + 1 - startByteIndex);
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



  }
  public static class CachedServerHandshake {
    private String sessionID;
    public byte[] modListPacket;
    public List<byte[]> otherPackets;

    private CachedServerHandshake(String sessionID,byte[] modListPacket,List<byte[]> otherPackets) {
      this.sessionID = sessionID;
      this.modListPacket = modListPacket;
      this.otherPackets = otherPackets;
    }
  }
}
