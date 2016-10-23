package com.middlerim.integration;

import com.middlerim.client.view.Logger;

public class IntegrationLogger implements Logger {
  @Override
  public void warn(String tag, String message) {
    System.out.println("[" + tag + "] WARN " + message);
  }

  @Override
  public void debug(String tag, String message) {
    System.out.println("[" + tag + "] DEBUG " + message);
  }
}
