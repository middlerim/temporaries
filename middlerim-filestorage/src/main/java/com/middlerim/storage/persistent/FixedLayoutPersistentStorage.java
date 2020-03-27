package com.middlerim.storage.persistent;

import java.io.Closeable;
import java.io.IOException;

import com.middlerim.Config;

import io.netty.util.concurrent.DefaultThreadFactory;

public class FixedLayoutPersistentStorage<L extends FixedLayout> implements Closeable {
  private BackgroundService<L> backgroundService;
  private FixedLayoutSegments<L> segments;

  public <I extends FixedLayoutStorageInformation> FixedLayoutPersistentStorage(I info) {
    BackgroundService.closeOnShutdown(this);
    this.segments = new FixedLayoutSegments<>(info);
    this.backgroundService = new BackgroundService<>(new BackgroundService.Callback<L>() {
      @Override
      public void put(L layout) throws IOException {
        segments.put(layout);
      }

      @Override
      public L get(Long id, L layout) throws IOException {
        return segments.get(id, layout);
      }

      @Override
      public void delete(Long id) throws IOException {
        segments.delete(id);
      }
      
    }, new DefaultThreadFactory(Config.INTERNAL_APP_NAME + "-" + info.storageId()));
  }

  @Override
  public void close() {
    backgroundService.close();
    segments.close();
  }

  public void put(L layout) {
    backgroundService.put(layout);
  }

  public L get(Long id, L layout) {
    return backgroundService.get(id, layout);
  }

  public void delete(Long id) {
    backgroundService.delete(id);
  }
}
