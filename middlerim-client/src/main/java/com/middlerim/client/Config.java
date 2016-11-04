package com.middlerim.client;

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
}
