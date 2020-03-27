package com.middlerim.client;

import java.net.InetSocketAddress;

public final class Config extends com.middlerim.Config {

  private Config() {
  }
  public static final int MAX_MESSAGE_WAIT_MILLIS = 1000;
  public static final int MAX_MESSAGE_QUEUE = 10;
  public static final int MIN_RETRY_RERIOD_MILLIS = 1000;
  public static final int MAX_RETRY_RERIOD_MILLIS = 10 * 60 * 1000;
  
  public static final int ALLOWABLE_MARGIN_LOCATION_METER = 10;
  public static final int ABANDONED_MESSAGE_THREASHOLD = 10;
  public static final int MAX_MESSAGE_CACHE_SIZE = 100;

  public static final String KEY_KEEP_ALIVE_ENABLED = "middlerim.keep.alive";
  public static final boolean KEEP_ALIVE_ENABLED = getBooleanValue(KEY_KEEP_ALIVE_ENABLED, true);

  public static final String COMMAND_SERVER_HOST = "127.0.0.1";
  public static final InetSocketAddress COMMAND_SERVER = new InetSocketAddress(Config.COMMAND_SERVER_HOST, Config.COMMAND_SERVER_PORT);

}
