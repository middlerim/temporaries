package com.middlerim.server.storage.persistent;

import java.nio.file.Path;
import java.nio.file.Paths;

import com.middlerim.server.Config;

public class StorageInformation<L extends Persistent<L>> {

  private final String storageId;
  private final int recordSize;
  private final long maxStorageSize;

  public StorageInformation(String storageId, int recordSize, long maxStorageSize) {
    this.storageId = storageId;
    this.recordSize = recordSize;
    this.maxStorageSize = maxStorageSize;
  }

  public String storageId() {
    return storageId;
  }

  public int recordSize() {
    return recordSize;
  }

  public long maxStorageSize() {
    return maxStorageSize;
  }

  public Path path() {
    return Paths.get("./", Config.TEST ? "db_test" : "db", "s_" + storageId);
  }
}
