package org.adde0109.ambassador.forge;

import com.velocitypowered.proxy.connection.ConnectionType;

public class ForgeConstants {
  public static final String HANDLER = "Modern Forge handler";
  public static final String MARKER_ADDER = "FML2/3 Marker Adder";
  public static final String RESET_LISTENER = "ambassador-reset-listener";
  public static final String SERVER_SUCCESS_LISTENER = "ambassador-server-success-listener";
  public static final String FORGE_HANDSHAKE_HOLDER = "ambassador-forge-handshake-holder";

  public static final String FML2Marker = "\0FML2\0";
  public static final String FML3Marker = "\0FML3\0";
  public static final ConnectionType ForgeFML2 = new ForgeFMLConnectionType(2);
  public static final ConnectionType ForgeFML3 = new ForgeFMLConnectionType(3);
}
