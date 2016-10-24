package com.middlerim.storage.persistent;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.StandardOpenOption;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IdStorage implements Closeable {
  private static final Logger LOG = LoggerFactory.getLogger(IdStorage.class);

  private final IdStorageInformation info;
  private FileChannel fc;
  private final ByteBuffer buf;

  public IdStorage(IdStorageInformation info) {
    BackgroundService.closeOnShutdown(this);
    this.info = info;
    if (info.minValue() < 0) {
      throw new IllegalArgumentException("minValue must be greater than or equal to 0. minValue: " + info.minValue());
    }
    if (info.maxValue() < info.minValue()) {
      throw new IllegalArgumentException("maxValue must be greater than minValue. maxValue: " + info.maxValue() + ", minValue: " + info.minValue());
    }
    try {
      fc = getChannel();
    } catch (IOException e) {
      throw new RuntimeException("Could not open or create file. path: " + info.path(), e);
    }
    this.buf = ByteBuffer.allocate(8);
    this.buf.limit(8);
  }

  private FileChannel getChannel() throws IOException {
    File dir = info.path().toFile();
    if (!dir.exists()) {
      dir.mkdirs();
    }
    File path = new File(dir, "data");
    FileChannel fc = FileChannel.open(path.toPath(), StandardOpenOption.CREATE, StandardOpenOption.DSYNC, StandardOpenOption.READ, StandardOpenOption.WRITE);
    return fc;
  }

  @Override
  public void close() {
    if (fc != null) {
      close(fc);
    }
  }

  private void close(FileChannel fc) {
    try {
      fc.force(true);
    } catch (IOException e) {
      // Do not use logger otherwise deadlock or something unexpected things will be happened.
      System.err.println("Could not force flash the storage. It might have lost some data. FileChannel: " + fc);
      e.printStackTrace(System.err);
    }
    try {
      fc.close();
    } catch (IOException e) {
      if (fc.isOpen()) {
        try {
          fc.close();
        } catch (IOException e1) {
          // Do not use logger otherwise deadlock or something unexpected things will be happened.
          LOG.error("Could not close the storage. It might have lost some data. FileChannel: " + fc);
          e1.printStackTrace(System.err);
        }
      }
    }
  }

  public synchronized long incrementAndGet() throws IOException {
    buf.position(0);
    fc.read(buf, 0);
    long id;
    if (buf.position() != 0) {
      id = buf.getLong(0);
    } else {
      id = info.minValue();
    }
    if (id < info.minValue() || id > info.maxValue()) {
      id = info.minValue();
    } else {
      ++id;
    }
    buf.putLong(0, id).position(0);
    fc.write(buf, 0);
    return id;
  }
}
