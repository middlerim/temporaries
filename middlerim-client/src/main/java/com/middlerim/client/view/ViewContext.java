package com.middlerim.client.view;

import java.io.File;

public abstract class ViewContext {

  public abstract File getCacheDir();

  public abstract boolean isDebug();
  
  public abstract MessagePool<?> getMessagePool();
}
