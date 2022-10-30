package org.adde0109.ambassador.forge;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerPing;
import com.velocitypowered.api.util.ModInfo;
import java.util.*;

import com.velocitypowered.api.util.UuidUtils;
import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.config.PlayerInfoForwarding;
import com.velocitypowered.proxy.config.VelocityConfiguration;
import com.velocitypowered.proxy.connection.MinecraftConnection;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import com.velocitypowered.proxy.protocol.StateRegistry;
import com.velocitypowered.proxy.protocol.packet.ServerLoginSuccess;
import io.netty.buffer.ByteBuf;
import org.adde0109.ambassador.velocity.VelocityForgeHandshakeSessionHandler;
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

    ByteArrayDataOutput stream = ByteStreams.newDataOutput();
    byte[] dataAndPacketId = dataAndPacketIdStream.toByteArray();
    writeUtf(stream,"fml:handshake");
    writeVarInt(stream,dataAndPacketId.length);
    stream.write(dataAndPacketId);
    return stream.toByteArray();
  }
  public static byte[] generatePluginResetPacket() {
    ByteArrayDataOutput dataAndPacketIdStream = ByteStreams.newDataOutput();
    writeVarInt(dataAndPacketIdStream,98);
    return dataAndPacketIdStream.toByteArray();
  }

  public static final byte[] emptyModlist = generateEmptyModlist();
  private static byte[] generateEmptyModlist() {
    ByteArrayDataOutput dataAndPacketIdStream = ByteStreams.newDataOutput();
    writeVarInt(dataAndPacketIdStream,1);
    writeVarInt(dataAndPacketIdStream,0);
    writeVarInt(dataAndPacketIdStream,0);
    writeVarInt(dataAndPacketIdStream,0);

    ByteArrayDataOutput stream = ByteStreams.newDataOutput();
    byte[] dataAndPacketId = dataAndPacketIdStream.toByteArray();
    writeUtf(stream,"fml:handshake");
    writeVarInt(stream,dataAndPacketId.length);
    stream.write(dataAndPacketId);
    return stream.toByteArray();
  }


  public static final byte[] ACKPacket = generateACKPacket();
  private static byte[] generateACKPacket() {
    ByteArrayDataOutput dataAndPacketIdStream = ByteStreams.newDataOutput();
    writeVarInt(dataAndPacketIdStream,99);

    ByteArrayDataOutput stream = ByteStreams.newDataOutput();
    byte[] dataAndPacketId = dataAndPacketIdStream.toByteArray();
    writeUtf(stream,"fml:handshake");
    writeVarInt(stream,dataAndPacketId.length);
    stream.write(dataAndPacketId);
    return stream.toByteArray();
  }
}
