package com.middlerim.client.view;

import java.io.File;

public abstract class ViewContext {

  public abstract File getCacheDir();

  public abstract Logger logger();

  public abstract boolean isDebug();
}
