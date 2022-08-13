package org.adde0109.ambassador.forge;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerPing;
import com.velocitypowered.api.util.ModInfo;
import java.util.*;

import io.netty.buffer.ByteBuf;
import org.slf4j.Logger;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

public class ForgeHandshakeUtils {

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

  public static void writeVarInt(ByteArrayDataOutput stream,int i) {
    while((i & -128) != 0) {
      stream.writeByte(i & 127 | 128);
      i >>>= 7;
    }

    stream.writeByte(i);
  }

  public static void writeUtf(ByteArrayDataOutput stream,String p_211400_1_) {
    byte[] abyte = p_211400_1_.getBytes(StandardCharsets.UTF_8);
      writeVarInt(stream,abyte.length);
      stream.write(abyte);
  }

  public static byte[] generateTestPacket() {
    ByteArrayDataOutput dataAndPacketIdStream = ByteStreams.newDataOutput();
    writeVarInt(dataAndPacketIdStream,4);
    writeUtf(dataAndPacketIdStream,"ambassadortestpacket");
    writeVarInt(dataAndPacketIdStream,0);

    ByteArrayDataOutput stream = ByteStreams.newDataOutput();
    byte[] dataAndPacketId = dataAndPacketIdStream.toByteArray();
    writeUtf(stream,"fml:handshake");
    writeVarInt(stream,dataAndPacketId.length);
    stream.write(dataAndPacketId);

    return stream.toByteArray();
  }

  public static byte[] generateResetPacket() {
    ByteArrayDataOutput dataAndPacketIdStream = ByteStreams.newDataOutput();
    writeVarInt(dataAndPacketIdStream,98);
    return dataAndPacketIdStream.toByteArray();
  }

  public static class HandshakeReceiver {

    private int partLength;

    public static Logger logger;

    private int numberOfRecivedParts;
    private int recivedBytes;

    private final int numberOfParts;
    private final long checksum;
    private final int[] separators;
    private final byte[] recivedParts;


    private HandshakeReceiver(ServerPing serverPing) throws Exception {
      if ((serverPing.getModinfo().isEmpty()) || (!Objects.equals(serverPing.getModinfo().get().getType(), "ambassador"))) {
        throw new HandshakeNotAvailableException("The specified Forge server is not running the Ambassador-Forge mod!");
      }

      ModInfo.Mod pair = serverPing.getModinfo().orElseThrow(IllegalAccessError::new).getMods().get(0);

      this.separators = Arrays.stream(pair.getVersion().substring(pair.getVersion().indexOf(":") + 1).split(":")).map(Integer::parseInt).mapToInt(x -> x).toArray();
      this.checksum = Long.parseUnsignedLong((pair.getVersion().split(":")[0].split("-"))[3],16);
      this.numberOfParts = Integer.parseInt((pair.getVersion().split(":")[0].split("-"))[1]);
      int totalLength = Integer.parseInt((pair.getVersion().split(":")[0].split("-"))[2]);
      this.recivedParts = new byte[totalLength];
    }



    public static CompletableFuture<CachedServerHandshake> downloadHandshake(RegisteredServer forgeServer) {
      CompletableFuture<CachedServerHandshake> future = new CompletableFuture<>();
      forgeServer.ping().whenComplete((msg,ex) -> {
        if (ex != null) {
          future.completeExceptionally(ex);
        } else {
          try {
            HandshakeReceiver handshakeReceiver = new HandshakeReceiver(msg);
            handshakeReceiver.handle(msg);
            handshakeReceiver.downloadLoop(forgeServer,future);
          } catch (Exception e) {
            future.completeExceptionally(e);
          }
        }
      });
      return future;
    }

    public static CompletableFuture<CachedServerHandshake> downloadHandshake(RegisteredServer forgeServer, CachedServerHandshake oldHandshake) {
      CompletableFuture<CachedServerHandshake> future = new CompletableFuture<>();
      forgeServer.ping().whenComplete((msg,ex) -> {
        if (ex != null) {
          future.completeExceptionally(ex);
        } else {
          try {
            HandshakeReceiver handshakeReceiver = new HandshakeReceiver(msg);
            if (handshakeReceiver.getChecksum() == oldHandshake.fingerprint) {
              future.complete(oldHandshake);
            } else {
              handshakeReceiver.handle(msg);
              handshakeReceiver.downloadLoop(forgeServer,future);
            }
          } catch (Exception e) {
            future.completeExceptionally(e);
          }
        }
      });
      return future;
    }

    private long getChecksum() {
      return checksum;
    }

    private void downloadLoop(RegisteredServer server, CompletableFuture<CachedServerHandshake> future) {
      if (numberOfRecivedParts < numberOfParts) {
        server.ping().whenComplete((msg,ex) -> {
          if (ex != null) {
            future.completeExceptionally(ex);
          } else {
            handle(msg);
            downloadLoop(server, future);
          }
        });
      } else {
        List<byte[]> packets = splitPackets(recivedParts,separators);
        future.complete(new CachedServerHandshake(checksum,packets.get(0),packets.subList(1,packets.size()-1)));
      }
    }


    private void handle(ServerPing status) {
      numberOfRecivedParts++;

      ModInfo.Mod pair = status.getModinfo().orElseThrow(IllegalAccessError::new).getMods().get(0);
      int recivedPartNr = Integer.parseInt((pair.getVersion().split(":")[0].split("-"))[0]);
      placePartInArray(pair.getId().getBytes(StandardCharsets.ISO_8859_1), recivedPartNr - 1);

      logger.info("Downloaded part " + numberOfRecivedParts + " out of " + numberOfParts);
    }



    private void placePartInArray(byte[] temp, int partNr) {
      int head = (partNr == numberOfParts-1) ? recivedParts.length-temp.length : partNr*temp.length;
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

    public static class HandshakeNotAvailableException extends Exception {
      HandshakeNotAvailableException(String errorMessage) {
        super(errorMessage);
      }
    }

  }
  public static class CachedServerHandshake {
    private final long fingerprint;
    public byte[] modListPacket;
    public List<byte[]> otherPackets;

    private CachedServerHandshake(long fingerprint,byte[] modListPacket,List<byte[]> otherPackets) {
      this.fingerprint = fingerprint;
      this.modListPacket = modListPacket;
      this.otherPackets = otherPackets;
    }

    public boolean equals(CachedServerHandshake cachedServerHandshake) {
      return this.fingerprint == cachedServerHandshake.fingerprint;
    }
  }
}
