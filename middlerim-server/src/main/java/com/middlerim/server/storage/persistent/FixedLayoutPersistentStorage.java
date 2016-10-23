package com.middlerim.server.storage.persistent;

public class FixedLayoutPersistentStorage<L extends Persistent<L>> {
  private BackgroundService<L> backgroundService;
  private Segments<L> segments;

  public <I extends StorageInformation<L>> FixedLayoutPersistentStorage(I info) {
    this.segments = new Segments<>(info);
    this.backgroundService = new BackgroundService<>(segments);
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
