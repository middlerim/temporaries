package com.middlerim.server.command;

import java.nio.charset.Charset;

public final class Config extends com.middlerim.server.Config {
  public static final int MAX_ACTIVE_SESSION_SIZE = 100_000_000;
  public static final int MAX_RECEIVE_BUFFER_SIZE = MAX_COMMAND_BYTE * 1000;

  public static final int MAX_MESSAGE_CACHE_SIZE = 1_000_000;

  public static final int UNREACHED_MESSAGE_SURVIVAL_MINS = 5;
  public static final int UNREACHED_MESSAGE_RETRY_PERIOD_MILLIS = 5000;
  public static final int UNREACHED_MESSAGE_MAX_RETRY_COUNT = 3;

  public static final Charset CHARSET_MESSAGE = Charset.forName("utf-8");
}
