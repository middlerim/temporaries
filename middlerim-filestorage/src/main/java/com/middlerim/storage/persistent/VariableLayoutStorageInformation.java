package com.middlerim.storage.persistent;

import java.io.File;

import com.middlerim.Config;

public class VariableLayoutStorageInformation {

  private final String storageId;
  private final long maxStorageSize;
  private final int segmentSize;

  public VariableLayoutStorageInformation(String storageId, long maxStorageSize, int segmentSize) {
    this.storageId = storageId;
    this.maxStorageSize = maxStorageSize;
    this.segmentSize = segmentSize;
  }

  public String storageId() {
    return storageId;
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
