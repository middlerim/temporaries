package com.middlerim.client;

import java.nio.charset.Charset;

public final class Config {

  private Config() {
  }

  public static final int MAX_MESSAGE_WAIT_MILLIS = 1000;
  public static final int MAX_MESSAGE_QUEUE = 10;
  public static final long MIN_RETRY_RERIOD_MILLIS = 1000;
  public static final long MAX_RETRY_RERIOD_MILLIS = 10 * 60 * 1000;
  
  public static final int ALLOWABLE_MARGIN_LOCATION_METER = 10;
  public static final int ABANDONED_MESSAGE_THREASHOLD = 10;

  public static final int MAX_PACKET_SIZE = 1280;
  
  public static final Charset MESSAGE_ENCODING = Charset.forName("utf-8");
  public static final int MAX_MESSAGE_CACHE_SIZE = 100;
}
