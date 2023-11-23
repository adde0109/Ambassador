package org.adde0109.ambassador.forge.packet;

import com.velocitypowered.api.proxy.messages.ChannelIdentifier;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import com.velocitypowered.proxy.protocol.packet.LoginPluginResponse;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ModListReplyPacket implements IForgeLoginWrapperPacket<Context.ClientContext> {

  private List<String> mods;
  private Map<ChannelIdentifier, String> channels;
  private Map<String, String> registries;

  private final Context.ClientContext context;
  private ModListReplyPacket(List<String> mods, Map<ChannelIdentifier,
          String> channels, Map<String, String> registries, int id, boolean success) {
    this.mods = mods;
    this.channels = channels;
    this.registries = registries;
    this.context = Context.createContext(id, success);
  }

  public static ModListReplyPacket read(ByteBuf input, int msgID) {

    List<String> mods = new ArrayList<>();
    int len = ProtocolUtils.readVarInt(input);
    for (int x = 0; x < len; x++)
      mods.add(ProtocolUtils.readString(input, 0x100));

    Map<ChannelIdentifier, String> channels = new HashMap<>();
    len = ProtocolUtils.readVarInt(input);
    for (int x = 0; x < len; x++)
      channels.put(MinecraftChannelIdentifier.from(ProtocolUtils.readString(input, 32767)),
              ProtocolUtils.readString(input, 0x100));

    Map<String, String> registries = new HashMap<>();
    len = ProtocolUtils.readVarInt(input);
    for (int x = 0; x < len; x++)
      registries.put(ProtocolUtils.readString(input, 32767), ProtocolUtils.readString(input, 0x100));

    return new ModListReplyPacket(mods, channels, registries, msgID, true);
  }

  @Override
  public ByteBuf encode() {
    ByteBuf buf = Unpooled.buffer();

    ProtocolUtils.writeVarInt(buf, 2);

    ProtocolUtils.writeVarInt(buf, mods.size());
    mods.forEach(m -> ProtocolUtils.writeString(buf, m));

    ProtocolUtils.writeVarInt(buf, channels.size());
    channels.forEach((k, v) -> {
      ProtocolUtils.writeString(buf,k.getId());
      ProtocolUtils.writeString(buf,v);
    });

    ProtocolUtils.writeVarInt(buf, registries.size());
    registries.forEach((k, v) -> {
      ProtocolUtils.writeString(buf, k);
      ProtocolUtils.writeString(buf, v);
    });

    ByteBuf output = Unpooled.buffer();
    ProtocolUtils.writeString(output, "fml:handshake");
    ProtocolUtils.writeVarInt(output, buf.readableBytes());
    output.writeBytes(buf);

    return output;
  }

  @Override
  public Context.ClientContext getContext() {
    return context;
  }

  public List<String> getMods() {
    return mods;
  }

  public Map<ChannelIdentifier, String> getChannels() {
    return channels;
  }
}
