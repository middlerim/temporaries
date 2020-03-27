package com.middlerim.server;

import java.net.InetSocketAddress;

public abstract class Config extends com.middlerim.Config {
  public static final int MAX_ACTIVE_SESSION_SIZE = 100_000_000;

  public static final String COMMAND_SERVER_IPV4_HOST = "127.0.0.1";
  public static final String COMMAND_SERVER_IPV6_HOST = "fe80::1%lo0";
  public static final String SMALL_MEDIA_SERVER_IPV4_HOST = "127.0.0.1";

  public static final InetSocketAddress COMMAND_SERVER_IPV4 = new InetSocketAddress(Config.COMMAND_SERVER_IPV4_HOST, Config.COMMAND_SERVER_PORT);
  public static final InetSocketAddress COMMAND_SERVER_IPV6 = new InetSocketAddress(Config.COMMAND_SERVER_IPV6_HOST, Config.COMMAND_SERVER_PORT);
  public static final InetSocketAddress SMALL_MEDIA_SERVER_IPV4 = new InetSocketAddress(Config.SMALL_MEDIA_SERVER_IPV4_HOST, Config.SMALL_MEDIA_SERVER_PORT);

  public static final boolean isUnix;
  static {
    final String os = System.getProperty("os.name").toLowerCase();
    isUnix = (os.indexOf("nix") >= 0 || os.indexOf("nux") >= 0
        || os.indexOf("aix") > 0);
  }
}
