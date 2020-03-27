package com.middlerim.storage.persistent;

import java.io.File;

import com.middlerim.Config;

public class FixedLayoutStorageInformation {

  private final String storageId;
  private final int recordSize;
  private final long maxStorageSize;
  private final int segmentSize;

  public FixedLayoutStorageInformation(String storageId, int recordSize, long maxStorageSize, int segmentSize) {
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

  public File storage() {
    return new File(Config.TEST ? "./db_test" : "./db", "s_" + storageId);
  }
}
