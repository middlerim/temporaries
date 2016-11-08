package com.middlerim.server;

public abstract class Config extends com.middlerim.Config {
  public static final boolean isUnix;
  static {
    final String os = System.getProperty("os.name").toLowerCase();
    isUnix = (os.indexOf("nix") >= 0 || os.indexOf("nux") >= 0
        || os.indexOf("aix") > 0);
  }
}
