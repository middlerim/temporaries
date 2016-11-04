package com.middlerim;

import java.nio.charset.Charset;

public abstract class Config {

  public static boolean TEST = true;

  public static final String INTERNAL_APP_NAME = "mdlrm";
  public static final int MAX_PACKET_SIZE = 1280;
  public static final int MAX_DISPLAY_NAME_BYTE_LENGTH = 36;
  public static final Charset MESSAGE_ENCODING = Charset.forName("utf-8");

  public static final String CENTRAL_SERVER_IPV4_HOST = "192.168.101.5";
  public static final int CENTRAL_SERVER_IPV4_PORT = 1231;
  public static final String CENTRAL_SERVER_IPV6_HOST = "fe80::3e15:c2ff:fee6:8cc0";
  public static final int CENTRAL_SERVER_IPV6_PORT = 1232;

  public static final int SESSIONID_RENEW_CYCLE_MILLIS = getIntValue("middlerim.sessionid.renew.cycle.millis", 24 * 60 * 60 * 1000, 2 * 60 * 1000);
  public static final int SESSION_TIMEOUT_MILLIS = getIntValue("middlerim.session.timeout.millis", 30 * 60 * 1000, 1 * 60 * 1000);

  protected static int getIntValue(String envKey, int defaultValue, int testValue) {
    if (TEST) {
      return testValue;
    } else {
      String value = System.getenv(envKey);
      if (value != null && value.length() > 0) {
        return Integer.parseInt(value);
      } else {
        return defaultValue;
      }
    }
  }
  protected static long getLongValue(String envKey, long defaultValue, long testValue) {
    if (TEST) {
      return testValue;
    } else {
      String value = System.getenv(envKey);
      if (value != null && value.length() > 0) {
        return Long.parseLong(value);
      } else {
        return defaultValue;
      }
    }
  }
}
