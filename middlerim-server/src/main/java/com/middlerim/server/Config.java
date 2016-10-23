package com.middlerim.server;

import java.nio.charset.Charset;

public final class Config {

  private Config() {
  }

  public static final int MAX_ACTIVE_SESSION_SIZE = 100_000_000;
  public static final long SESSION_TIMEOUT_MILLIS = 24 * 60 * 60 * 1000;

  public static final int MAX_PACKET_SIZE = 512;
  public static final int MAX_RECEIVE_BUFFER_SIZE = MAX_PACKET_SIZE * 1000;
  
  public static final int MAX_MESSAGE_CACHE_SIZE = 1_000_000;

  public static final Charset CHARSET_MESSAGE = Charset.forName("utf-8");
  public static final String hostOne = "localhost";
  public static final int portOne = 1231;

  public static final String hostTwo = "10.0.2.15";
  // public static final String hostTwo = "localhost";
  public static final int portTwo = 1232;

  public static final long MAX_STORAGE_SEGMENT_SIZE = 1_000_000_000;

  public static boolean TEST = true;

  public static final boolean isUnix;
  static {
    final String os = System.getProperty("os.name").toLowerCase();
    isUnix = (os.indexOf("nix") >= 0 || os.indexOf("nux") >= 0
        || os.indexOf("aix") > 0);
  }
}
