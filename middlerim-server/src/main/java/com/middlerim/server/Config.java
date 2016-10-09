package com.middlerim.server;

import java.nio.charset.Charset;

/**
 * Peer to Peer Config
 */
public final class Config {

  private Config() {
  }

  public static final int MAX_SESSION_SIZE = 1000000;
  public static final long SESSION_TIMEOUT_MILLIS = 24 * 60 * 60 * 1000;

  public static final int MAX_MESSAGE_CACHE_SIZE = 1000000;

  public static final Charset CHARSET_MESSAGE = Charset.forName("utf-8");
  public static final String hostOne = "localhost";
  public static final int portOne = 1231;

  public static final String hostTwo = "10.0.2.15";
  // public static final String hostTwo = "localhost";
  public static final int portTwo = 1232;

  public static final boolean isUnix;
  static {
    final String os = System.getProperty("os.name").toLowerCase();
    isUnix = (os.indexOf("nix") >= 0 || os.indexOf("nux") >= 0
        || os.indexOf("aix") > 0);
  }
}
