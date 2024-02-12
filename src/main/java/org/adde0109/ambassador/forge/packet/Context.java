package org.adde0109.ambassador.forge.packet;

public class Context {

  private final int responseID;

  private final String channelName;

  private Context(int responseID, String channelName) {
    this.responseID = responseID;
      this.channelName = channelName;
  }

  public static Context createContext(int responseID, String channelName) {
    return new Context(responseID, channelName);
  }

  public static ClientContext createClientContext(int responseID, boolean clientSuccess, String channelName) {
    return new ClientContext(responseID, clientSuccess, channelName);
  }

  public static ClientContext fromContext(Context context, boolean clientSuccess) {
    return new ClientContext(context.responseID, clientSuccess, context.channelName);
  }

  public int getResponseID() {
    return responseID;
  }

  public String getChannelName() {
    return channelName;
  }

  public static class ClientContext extends Context {

    private final boolean clientSuccess;
    ClientContext(int responseID, boolean clientSuccess, String channelName) {
      super(responseID, channelName);
      this.clientSuccess = clientSuccess;
    }

    public boolean success() {
      return clientSuccess;
    }
  }
}
