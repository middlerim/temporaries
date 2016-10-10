package com.middlerim.server.storage.persistent;

import java.nio.file.Path;
import java.nio.file.Paths;

import com.middlerim.server.Config;

public class IdStorageInformation {

  private final String storageId;
  private final long minValue;
  private final long maxValue;

  public IdStorageInformation(String storageId, long minValue, long maxValue) {
    this.storageId = storageId;
    this.minValue = minValue;
    this.maxValue = maxValue;
  }

  public String storageId() {
    return storageId;
  }

  public long minValue() {
    return minValue;
  }

  public long maxValue() {
    return maxValue;
  }

  
  public Path path() {
    return Paths.get("./", Config.TEST ? "db_test" : "db", "s_" + storageId);
  }
}
