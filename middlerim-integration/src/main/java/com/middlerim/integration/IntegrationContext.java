package com.middlerim.integration;

import java.io.File;

import com.middlerim.client.CentralEvents;
import com.middlerim.client.view.Logger;
import com.middlerim.client.view.ViewContext;

public class IntegrationContext extends ViewContext {
  private final IntegrationLogger logger = new IntegrationLogger();
  private final File tmpDir = new File(".");
  public CentralEvents.ReceivedTextEvent lastReceivedTextEvent;
  public CentralEvents.ReceiveMessageEvent lastReceiveMessageEvent;

  @Override
  public File getCacheDir() {
    return tmpDir;
  }

  @Override
  public Logger logger() {
    return logger;
  }

  @Override
  public boolean isDebug() {
    return true;
  }
}
