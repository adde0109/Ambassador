package org.adde0109.ambassador.forge.pipeline;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import com.velocitypowered.proxy.network.Connections;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import com.velocitypowered.proxy.protocol.StateRegistry;
import com.velocitypowered.proxy.protocol.netty.MinecraftDecoder;
import com.velocitypowered.proxy.protocol.packet.AvailableCommands;
import com.velocitypowered.proxy.util.except.QuietRuntimeException;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.CorruptedFrameException;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.adde0109.ambassador.Ambassador;
import org.jetbrains.annotations.NotNull;

public class CommandDecoderErrorCatcher extends ChannelInboundHandlerAdapter {

  private final StateRegistry.PacketRegistry.ProtocolRegistry registry;

  private final ConnectedPlayer player;
  private boolean sentWarning = false;

  public CommandDecoderErrorCatcher(ProtocolVersion protocolVersion, ConnectedPlayer player) {
    this.registry = StateRegistry.PLAY.getProtocolRegistry(ProtocolUtils.Direction.CLIENTBOUND, protocolVersion);
    this.player = player;
  }

  @Override
  public void channelRead(@NotNull ChannelHandlerContext ctx, @NotNull Object msg) throws Exception {
    if (msg instanceof ByteBuf buf) {
      if (!ctx.channel().isActive() || !buf.isReadable()) {
        buf.release();
        return;
      }

      int originalReaderIndex = buf.readerIndex();
      int packetId = ProtocolUtils.readVarInt(buf);
      MinecraftPacket packet = registry.createPacket(packetId);
      buf.readerIndex(originalReaderIndex);
      if (packet instanceof AvailableCommands) {
        try {
          ((MinecraftDecoder) ctx.pipeline().get(Connections.MINECRAFT_DECODER)).channelRead(ctx, msg);
        } catch (QuietRuntimeException | CorruptedFrameException e) {
          RegisteredServer server = player.getConnectedServer().getServer();
          if (!Ambassador.getInstance().config.isSilenceWarnings() && !sentWarning) {
            player.sendMessage(Component.text("[Ambassador Warning]: Unsupported command argument type detected! " +
                    "Please install Proxy-Compatible-Forge mod on this backend server to have access to commands. " +
                    "This message can be silenced in the ambassador.toml config file.", NamedTextColor.YELLOW));
            sentWarning = true;
          }

        }

      } else {
        ctx.fireChannelRead(msg);
      }

    }
  }
}
