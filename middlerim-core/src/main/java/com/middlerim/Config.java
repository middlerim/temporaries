package com.middlerim;

import java.nio.charset.Charset;

public abstract class Config {

  public static boolean TEST = true;

  public static final String INTERNAL_APP_NAME = "mdlrm";
  public static final int MAX_PACKET_SIZE = 1280;
  public static final int MAX_DISPLAY_NAME_BYTE_LENGTH = 46;
  public static final Charset MESSAGE_ENCODING = Charset.forName("utf-8");

  public static final String CENTRAL_SERVER_IPV4_HOST = "192.168.101.7";
  public static final int CENTRAL_SERVER_IPV4_PORT = 1231;
  public static final String CENTRAL_SERVER_IPV6_HOST = "fe80::3e15:c2ff:fee6:8cc0";
  public static final int CENTRAL_SERVER_IPV6_PORT = 1232;

  public static final int SESSIONID_RENEW_CYCLE_MILLIS = getIntValue("middlerim.sessionid.renew.cycle.millis", 60 * 60 * 1000);
  public static final String KEY_SESSION_TIMEOUT_MILLIS = "middlerim.session.timeout.millis";
  public static final int SESSION_TIMEOUT_MILLIS = getIntValue(KEY_SESSION_TIMEOUT_MILLIS, 30 * 60 * 1000);

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
