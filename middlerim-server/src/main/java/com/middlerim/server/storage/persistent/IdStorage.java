package com.middlerim.server.storage.persistent;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IdStorage {
  private static final Logger LOG = LoggerFactory.getLogger(IdStorage.class);

  private final IdStorageInformation info;
  private FileChannel fc;
  private final ByteBuffer buf;

  public IdStorage(IdStorageInformation info) {
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
    this.buf = ByteBuffer.allocate(Long.BYTES);
    this.buf.position(0);
    this.buf.mark();
    BackgroundService.closeOnShutdown(fc);
  }

  private FileChannel getChannel() throws IOException {
    File dir = info.path().toFile();
    if (!dir.exists()) {
      dir.mkdirs();
    }
    FileChannel fc = null;
    File path = new File(dir, "data");
    @SuppressWarnings("resource")
    RandomAccessFile f = new RandomAccessFile(path, "rw");
    fc = f.getChannel();
    f.setLength(Long.BYTES);
    return fc;
  }

  public void close() {
    if (fc != null) {
      close(fc);
    }
  }

  private void close(FileChannel fc) {
    try {
      fc.force(true);
    } catch (IOException e) {
      LOG.error("Could not force flash the storage. It might have lost some data. FileChannel: {}", fc, e);
    }
    try {
      fc.close();
    } catch (IOException e) {
      if (fc.isOpen()) {
        try {
          fc.close();
        } catch (IOException e1) {
          LOG.error("Could not close the storage. It might have lost some data. FileChannel: {}", fc, e);
        }
      }
    }
  }

  public synchronized long incrementAndGet() throws IOException {
    buf.reset();
    fc.position(0).read(buf);
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
    buf.putLong(0, id);
    return id;
  }
  
  
}
