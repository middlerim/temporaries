package com.middlerim.storage.persistent;

import java.nio.file.Path;
import java.nio.file.Paths;

import com.middlerim.Config;

public class StorageInformation<L extends Persistent<L>> {

  private final String storageId;
  private final int recordSize;
  private final long maxStorageSize;
  private final int segmentSize;

  public StorageInformation(String storageId, int recordSize, long maxStorageSize, int segmentSize) {
    this.storageId = storageId;
    this.recordSize = recordSize;
    this.maxStorageSize = maxStorageSize;
    this.segmentSize = segmentSize;
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

  public long segmentSize() {
    return segmentSize;
  }

  public Path path() {
    return Paths.get("./", Config.TEST ? "db_test" : "db", "s_" + storageId);
  }
}
