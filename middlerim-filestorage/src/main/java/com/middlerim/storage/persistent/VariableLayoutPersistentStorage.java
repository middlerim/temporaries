package com.middlerim.storage.persistent;

import com.middlerim.Config;

import io.netty.util.concurrent.DefaultThreadFactory;

public class VariableLayoutPersistentStorage<L extends VariableLayout> {
  private BackgroundService<L> backgroundService;
  private VariableLayoutSegments<L> segments;

  public <I extends VariableLayoutStorageInformation> VariableLayoutPersistentStorage(I info) {
    this.segments = new VariableLayoutSegments<>(info);
    this.backgroundService = new BackgroundService<>(new BackgroundService.Callback<L>() {
      @Override
      public void put(L layout) throws Exception {
        segments.put(layout);
      }
      @Override
      public L get(Long id, L layout) throws Exception {
        return segments.get(id, layout);
      }
      @Override
      public void delete(Long id) throws Exception {
        segments.delete(id);
      }
    }, new DefaultThreadFactory(Config.INTERNAL_APP_NAME + "-" + info.storageId()));
  }

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
