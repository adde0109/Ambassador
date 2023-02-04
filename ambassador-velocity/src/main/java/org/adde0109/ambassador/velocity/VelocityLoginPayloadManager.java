package org.adde0109.ambassador.velocity;

import com.velocitypowered.proxy.connection.MinecraftConnection;
import com.velocitypowered.proxy.protocol.packet.LoginPluginMessage;
import com.velocitypowered.proxy.protocol.packet.LoginPluginResponse;
import io.netty.buffer.ByteBuf;

import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class VelocityLoginPayloadManager {
  private final HashMap<Integer, CompletableFuture<ByteBuf>> listenerList = new HashMap<>();
  private int counter = 0;
  private final MinecraftConnection connection;

  public VelocityLoginPayloadManager(MinecraftConnection connection) {
    this.connection = connection;
  }

  public CompletableFuture<ByteBuf> sendPayload(String channel, ByteBuf data) {
    connection.write(new LoginPluginMessage(counter,channel,data));
    CompletableFuture<ByteBuf> future = listenFor(counter);
    counter++;
    return future;
  }

  public CompletableFuture<ByteBuf> sendPayloads(String channel, List<ByteBuf> dataList) {
    final CompletableFuture<ByteBuf> callback = new CompletableFuture<>();
    for (ByteBuf data : dataList) {
      connection.delayedWrite(new LoginPluginMessage(counter, channel, data));
      listenerList.put(counter,callback);
      counter++;
    }
    connection.flush();
    return callback;
  }

  public CompletableFuture<ByteBuf> listenFor(int id) {
    CompletableFuture<ByteBuf> value = listenerList.get(id);
    if (value == null) {
      CompletableFuture<ByteBuf> callback = new CompletableFuture<>();
      listenerList.put(id,callback);
      return callback;
    } else {
      return value;
    }
  }

   boolean handlePayload(LoginPluginResponse response) {
    final CompletableFuture<ByteBuf> callback = listenerList.get(response.getId());
      if (callback != null) {
        listenerList.remove(response.getId());
        if (!listenerList.containsValue(callback)) {
          callback.complete(response.content());
        }
        return true;
      } else {
        return false;
      }
    }
  }
