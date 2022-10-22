package org.adde0109.ambassador.forge;

import com.velocitypowered.proxy.protocol.packet.LoginPluginMessage;
import com.velocitypowered.proxy.protocol.packet.ServerLoginSuccess;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.util.ReferenceCountUtil;

import java.util.*;

public class FML2CRPMOutboundCatcher extends ChannelOutboundHandlerAdapter {

  private final Map<ChannelPromise, Object> catchedPackets = Collections.synchronizedMap(new LinkedHashMap<>());

  @Override
  public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
    final Set<Map.Entry<ChannelPromise, Object>> s = catchedPackets.entrySet();
    Iterator<Map.Entry<ChannelPromise, Object>> i = s.iterator();
    if (!ctx.channel().isActive()) {
      while (catchedPackets.entrySet().iterator().hasNext()) {
        final Map.Entry<ChannelPromise, Object> entry = i.next();
        ReferenceCountUtil.release(entry.getValue());
        i.remove();
      }
    } else {
      while (catchedPackets.entrySet().iterator().hasNext()) {
        final Map.Entry<ChannelPromise, Object> entry = i.next();
        ctx.write(entry.getValue(),entry.getKey());
        i.remove();
      }
      ctx.flush();
    }
  }

  @Override
  public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
    if (msg instanceof LoginPluginMessage || msg instanceof ServerLoginSuccess) {
      ctx.write(msg, promise);
    } else {
      catchedPackets.put(promise,msg);
    }
  }
}
