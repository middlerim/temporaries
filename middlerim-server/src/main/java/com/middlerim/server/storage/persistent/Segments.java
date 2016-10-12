package com.middlerim.server.storage.persistent;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.middlerim.server.Config;

// (!) The instance is NOT thread-safe.
class Segments<L extends Persistent> {

  private static final Logger LOG = LoggerFactory.getLogger(Segments.class);

  private final StorageInformation<L> info;
  private final long segmentSize;
  private final FileChannel[] fcs;
  private final ByteBuffer putItem;
  private final ByteBuffer deletedItem;

  Segments(StorageInformation<L> info) {
    this.info = info;
    if (Config.TEST) {
      this.segmentSize = 160 - (160 % info.recordSize());
    } else if (info.maxStorageSize() > Config.MAX_STORAGE_SEGMENT_SIZE) {
      this.segmentSize = Config.MAX_STORAGE_SEGMENT_SIZE - (Config.MAX_STORAGE_SEGMENT_SIZE % info.recordSize());
    } else {
      this.segmentSize = info.maxStorageSize();
    }
    LOG.info("Created segments for {}: a segment size: {} byte, max storage size: {} byte", info.storageId(), segmentSize, info.maxStorageSize());
    this.fcs = new FileChannel[(int) (Math.floor((double) info.maxStorageSize() / this.segmentSize)) + 1];
    LOG.info("                max file channel size: {}", fcs.length);
    this.putItem = ByteBuffer.allocate(info.recordSize());
    this.putItem.position(0);
    this.putItem.mark();
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

  private FileChannel getChannel(long id) throws IOException {
    int chIndex = channelIndex(id);
    FileChannel fc = fcs[chIndex];
    if (fc == null) {
      File dir = info.path().toFile();
      if (!dir.exists()) {
        dir.mkdirs();
      }
      @SuppressWarnings("resource")
      RandomAccessFile f = new RandomAccessFile(new File(dir, "seg" + chIndex), "rw");
      f.setLength(segmentSize);
      fc = f.getChannel();
      fcs[chIndex] = fc;
      BackgroundService.closeOnShutdown(fc);
    } else if (!fc.isOpen()) {
      fcs[chIndex] = null;
      return getChannel(id);
    }
    return fc;
  }

  void put(L layout) throws IOException {
    long id = layout.id();
    FileChannel fc = getChannel(id);
    long offset = getOffset(id);
    try {
      putItem.reset();
      layout.read(putItem);
      putItem.reset();
      fc.position(offset).write(putItem);
    } catch (IOException e) {
      LOG.error("Could not write to the file. Data: {}, FileChannel: {}", layout, fc, e);
    }
  }

  void delete(long id) throws IOException {
    FileChannel fc = getChannel(id);
    long offset = getOffset(id);
    try {
      deletedItem.reset();
      fc.position(offset).write(deletedItem);
    } catch (IOException e) {
      LOG.error("Could not write a delete record row to the file. Id: {}, FileChannel: {}", id, fc, e);
    }
  }

  void close() {
    for (FileChannel fc : fcs) {
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
}
