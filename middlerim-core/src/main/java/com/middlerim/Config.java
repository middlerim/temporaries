package com.middlerim;

import java.nio.charset.Charset;

public abstract class Config {

  public static boolean TEST = true;

  public static final String INTERNAL_APP_NAME = "mdlrm";
  public static final int MAX_PACKET_SIZE = 1280;
  public static final int MAX_DISPLAY_NAME_BYTE_LENGTH = 36;
  public static final Charset MESSAGE_ENCODING = Charset.forName("utf-8");

  public static final String CENTRAL_SERVER_IPV4_HOST = "192.168.101.5";
  public static final int CENTRAL_SERVER_IPV4_PORT = 1231;
  public static final String CENTRAL_SERVER_IPV6_HOST = "fe80::3e15:c2ff:fee6:8cc0";
  public static final int CENTRAL_SERVER_IPV6_PORT = 1232;
}
