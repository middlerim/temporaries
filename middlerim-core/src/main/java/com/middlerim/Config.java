package com.middlerim;

import java.nio.charset.Charset;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class Config {
  private static final Logger LOG = LoggerFactory.getLogger("mdlrm");

  public static final String INTERNAL_APP_NAME = "mdlrm";
  public static final boolean PRODUCTION = getBooleanValue("middlerim.production", false);
  public static final boolean TEST = !PRODUCTION;

  // < 1280 to prevent IP fragmentation
  public static final int MAX_COMMAND_BYTE = 1280;
  public static final int MAX_SMALLMEDIA_BYTE = 2_560_000;
  public static final int MAX_DISPLAY_NAME_BYTE_LENGTH = 46;
  public static final int MAX_TEXT_BYTES = 800;
  public static final Charset MESSAGE_ENCODING = Charset.forName("utf-8");

  public static final int COMMAND_SERVER_PORT = 1235;
  public static final int SMALL_MEDIA_SERVER_PORT = 1236;

  public static final int SESSIONID_RENEW_CYCLE_MILLIS = getIntValue("middlerim.sessionid.renew.cycle.millis", 60 * 60 * 1000);
  public static final String KEY_SESSION_TIMEOUT_MILLIS = "middlerim.session.timeout.millis";
  public static final int SESSION_TIMEOUT_MILLIS = getIntValue(KEY_SESSION_TIMEOUT_MILLIS, 30 * 60 * 1000);

  static {
    LOG.info("SESSIONID_RENEW_CYCLE_MILLIS={}", SESSIONID_RENEW_CYCLE_MILLIS);
    LOG.info("SESSION_TIMEOUT_MILLIS={}", SESSION_TIMEOUT_MILLIS);
  }

  protected static boolean getBooleanValue(String envKey, boolean defaultValue) {
    String value = getSystemValue(envKey);
    if (value != null && value.length() > 0) {
      return Boolean.parseBoolean(value);
    } else {
      return defaultValue;
    }
  }
  protected static int getIntValue(String envKey, int defaultValue) {
    String value = getSystemValue(envKey);
    if (value != null && value.length() > 0) {
      return Integer.parseInt(value);
    } else {
      return defaultValue;
    }
  }
  protected static long getLongValue(String envKey, long defaultValue) {
    String value = getSystemValue(envKey);
    if (value != null && value.length() > 0) {
      return Long.parseLong(value);
    } else {
      return defaultValue;
    }
  }
  private static String getSystemValue(String key) {
    String value = System.getProperty(key);
    if (value != null && !value.isEmpty()) {
      return value;
    }
    return System.getenv(key);
  }
}
