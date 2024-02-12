package org.adde0109.ambassador.forge;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.adde0109.ambassador.forge.packet.Context;
import org.adde0109.ambassador.forge.packet.GenericForgeLoginWrapperPacket;
import org.adde0109.ambassador.forge.packet.IForgeLoginWrapperPacket;

import java.nio.charset.StandardCharsets;

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

  public static final byte[] emptyModlistFML2 = generateEmptyModlist(2);
  public static final byte[] emptyModlistFML3 = generateEmptyModlist(3);
  private static byte[] generateEmptyModlist(int fmlVersion) {
    ByteArrayDataOutput dataAndPacketIdStream = ByteStreams.newDataOutput();
    writeVarInt(dataAndPacketIdStream,1);
    writeVarInt(dataAndPacketIdStream,0); //Mods
    if (fmlVersion == 3) {
      writeVarInt(dataAndPacketIdStream,1); //Channels
      writeUtf(dataAndPacketIdStream, "forge:tier_sorting");
      writeUtf(dataAndPacketIdStream,"1.0");
      writeVarInt(dataAndPacketIdStream,0); //Registries
      writeVarInt(dataAndPacketIdStream,0); //Data-packs
    } else {
      writeVarInt(dataAndPacketIdStream,0); //Channels
      writeVarInt(dataAndPacketIdStream,0); //Registries
    }


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

  public static class ThirdPartyRegistryUtils {

    static enum ThirdPartyChannel {
      SILENTGEAR_NETWORK {
        @Override
        public ThirdPartyRegistryUtils.ACKPacket generateResponsePacket(Context.ClientContext context) {
          return new ACKPacket(context, 3);
        }
      },
      ZETA_MAIN {
        @Override
        public ThirdPartyRegistryUtils.ACKPacket generateResponsePacket(Context.ClientContext context) {
          return new ACKPacket(context, 99);
        }
      };
      abstract public ACKPacket generateResponsePacket(Context.ClientContext context);
    }

    static boolean isThirdPartyPacket(GenericForgeLoginWrapperPacket<Context> packet) {
      try {
        Enum.valueOf(ThirdPartyChannel.class,
                packet.getContext().getChannelName().replace(':', '_').toUpperCase());
        return true;
      } catch (IllegalArgumentException e) {
        return false;
      }
    }

    static ThirdPartyChannel getThirdPartyChannel(GenericForgeLoginWrapperPacket<Context> packet) {
      return Enum.valueOf(ThirdPartyChannel.class,
              packet.getContext().getChannelName().replace(':', '_').toUpperCase());
    }

    static class ACKPacket implements IForgeLoginWrapperPacket<Context.ClientContext> {

      private final Context.ClientContext context;
      private final int packetID;
      ACKPacket(Context.ClientContext context, int packetID) {
        this.context = context;
        this.packetID = packetID;
      }

      public ByteBuf encode() {
        ByteBuf buf = Unpooled.buffer();

        ProtocolUtils.writeVarInt(buf, packetID);

        return buf;
      }

      @Override
      public Context.ClientContext getContext() {
        return null;
      }
    }
  }
}
