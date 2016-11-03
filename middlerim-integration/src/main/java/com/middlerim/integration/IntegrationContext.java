package com.middlerim.integration;

import java.io.File;

import com.middlerim.client.CentralEvents;
import com.middlerim.client.view.ViewContext;

public class IntegrationContext extends ViewContext {
  private final File tmpDir = new File(".");
  public CentralEvents.ReceivedTextEvent lastReceivedTextEvent;
  public CentralEvents.ReceiveMessageEvent lastReceiveMessageEvent;

  @Override
  public File getCacheDir() {
    return tmpDir;
  }

  @Override
  public boolean isDebug() {
    return true;
  }
}
