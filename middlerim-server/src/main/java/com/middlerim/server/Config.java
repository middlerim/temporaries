package com.middlerim.server;

import java.nio.charset.Charset;

public final class Config extends com.middlerim.Config {

  private Config() {
  }

  public static final int MAX_ACTIVE_SESSION_SIZE = 100_000_000;
  public static final int MAX_RECEIVE_BUFFER_SIZE = MAX_PACKET_SIZE * 1000;

  public static final int MAX_MESSAGE_CACHE_SIZE = 1_000_000;

  public static final Charset CHARSET_MESSAGE = Charset.forName("utf-8");

  public static final boolean isUnix;
  static {
    final String os = System.getProperty("os.name").toLowerCase();
    isUnix = (os.indexOf("nix") >= 0 || os.indexOf("nux") >= 0
        || os.indexOf("aix") > 0);
  }
}
