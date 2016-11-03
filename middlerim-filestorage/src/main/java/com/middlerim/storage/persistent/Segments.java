package com.middlerim.storage.persistent;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// (!) The instance is NOT thread-safe.
class Segments<L extends Persistent<L>> implements Closeable {

  private static final Logger LOG = LoggerFactory.getLogger(Segments.class);

  private final StorageInformation<L> info;
  private final long segmentSize;
  private final FileChannel[] fcs;
  private final ByteBuffer putItem;
  private final ByteBuffer getItem;
  private final ByteBuffer deletedItem;

  Segments(StorageInformation<L> info) {
    BackgroundService.closeOnShutdown(this);
    this.info = info;
    if (info.maxStorageSize() > info.segmentSize()) {
      this.segmentSize = info.segmentSize() - (info.segmentSize() % info.recordSize());
    } else {
      this.segmentSize = info.maxStorageSize();
    }
    LOG.info("Created segments for {}: a segment size: {} byte, max storage size: {} byte", info.storageId(), segmentSize, info.maxStorageSize());
    this.fcs = new FileChannel[(int) (Math.floor((double) info.maxStorageSize() / this.segmentSize)) + 1];
    LOG.info("                max file channel size: {}", fcs.length);
    this.putItem = ByteBuffer.allocate(info.recordSize());
    this.putItem.position(0);
    this.putItem.mark();

    this.getItem = ByteBuffer.allocate(info.recordSize());
    this.getItem.position(0);
    this.getItem.mark();

    byte[] deletedBs = new byte[info.recordSize()];
    Arrays.fill(deletedBs, (byte) 0b0);
    this.deletedItem = ByteBuffer.allocate(info.recordSize()).put(deletedBs);
    this.deletedItem.position(0);
    this.deletedItem.mark();
  }

  private int channelIndex(long id) {
    return (int) (info.recordSize() * id / segmentSize);
  }

  private long getOffset(long id) {
    return (info.recordSize() * id) % segmentSize;
  }

  private FileChannel getChannel(long id, boolean createIfNot) throws IOException {
    int chIndex = channelIndex(id);
    FileChannel fc = fcs[chIndex];
    if (fc == null) {
      File dir = info.storage();
      if (!dir.exists()) {
        if (!createIfNot) {
          return null;
        }
        dir.mkdirs();
      }
      File file = new File(dir, "seg" + chIndex);
      if (!createIfNot && !file.exists()) {
        return null;
      }
      @SuppressWarnings("resource")
      RandomAccessFile f = new RandomAccessFile(file, "rw");
      f.setLength(segmentSize);
      fc = f.getChannel();
      fcs[chIndex] = fc;
    } else if (!fc.isOpen()) {
      fcs[chIndex] = null;
      return getChannel(id, createIfNot);
    }
    return fc;
  }

  void put(L layout) throws IOException {
    long id = layout.id();
    FileChannel fc = getChannel(id, true);
    long offset = getOffset(id);
    try {
      putItem.reset();
      layout.read(putItem);
      putItem.reset();
      fc.write(putItem, offset);
    } catch (IOException e) {
      LOG.error("Could not write to the file. Data: {}, FileChannel: {}", layout, fc, e);
    }
  }

  L get(long id, L layout) throws IOException {
    FileChannel fc = getChannel(id, false);
    if (fc == null) {
      return null;
    }
    long offset = getOffset(id);
    synchronized (getItem) {
      getItem.reset();
      fc.read(getItem, offset);
      getItem.reset();
      if (getItem.remaining() <= 0) {
        return null;
      }
      layout.write(getItem);
    }
    return layout;
  }

  void delete(long id) throws IOException {
    FileChannel fc = getChannel(id, true);
    long offset = getOffset(id);
    try {
      deletedItem.reset();
      fc.write(deletedItem, offset);
    } catch (IOException e) {
      LOG.error("Could not write a delete record row to the file. Id: {}, FileChannel: {}", id, fc, e);
    }
  }

  @Override
  public void close() {
    for (FileChannel fc : fcs) {
      close(fc);
    }
  }

  private void close(FileChannel fc) {
    if (fc == null || !fc.isOpen()) {
      return;
    }
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
}
