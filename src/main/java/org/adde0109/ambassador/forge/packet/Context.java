package org.adde0109.ambassador.forge.packet;

public class Context {

  private final int responseID;

  private Context(int responseID) {
    this.responseID = responseID;
  }

  public static Context createContext(int responseID) {
    return new Context(responseID);
  }

  public static ClientContext createClientContext(int responseID, boolean clientSuccess) {
    return new ClientContext(responseID,clientSuccess);
  }

  public static ClientContext fromContext(Context context, boolean clientSuccess) {
    return new ClientContext(context.responseID,clientSuccess);
  }

  public int getResponseID() {
    return responseID;
  }

  public static class ClientContext extends Context {

    private final boolean clientSuccess;
    ClientContext(int responseID, boolean clientSuccess) {
      super(responseID);
      this.clientSuccess = clientSuccess;
    }

    public boolean success() {
      return clientSuccess;
    }
  }
}
