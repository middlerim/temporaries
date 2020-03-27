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

class FixedLayoutSegments<L extends FixedLayout> implements Closeable {

  private static final Logger LOG = LoggerFactory.getLogger(FixedLayoutSegments.class);

  private final FixedLayoutStorageInformation info;
  private final long segmentSize;
  private final FileBuffer[] pool;
  private final ByteBuffer deletedItem;

  FixedLayoutSegments(FixedLayoutStorageInformation info) {
    BackgroundService.closeOnShutdown(this);
    this.info = info;
    if (info.maxStorageSize() > info.segmentSize()) {
      this.segmentSize = info.segmentSize() - (info.segmentSize() % info.recordSize());
    } else {
      this.segmentSize = info.maxStorageSize();
    }
    LOG.info("Created segments for {}: a segment size: {} byte, max storage size: {} byte", info.storageId(), segmentSize, info.maxStorageSize());
    this.pool = new FileBuffer[(int) (Math.floor((double) info.maxStorageSize() / this.segmentSize)) + 1];
    LOG.info("                max file channel size: {}", pool.length);
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

  private FileBuffer getChannel(long id, boolean createIfNot) throws IOException {
    int chIndex = channelIndex(id);
    FileBuffer fb = pool[chIndex];
    if (fb == null) {
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
      fb = new FileBuffer(f.getChannel(), ByteBuffer.allocateDirect(info.recordSize()));
      pool[chIndex] = fb;
    } else if (!fb.fc.isOpen()) {
      pool[chIndex] = null;
      return getChannel(id, createIfNot);
    }
    return fb;
  }

  void put(L layout) throws IOException {
    long id = layout.id();
    FileBuffer fb = getChannel(id, true);
    long offset = getOffset(id);
    try {
      synchronized (fb) {
        fb.buf.position(0);
        layout.read(fb.buf);
        fb.buf.position(0);
        fb.fc.write(fb.buf, offset);
      }
    } catch (IOException e) {
      LOG.error("Could not write to the file. Data: {}, FileChannel: {}", layout, fb.fc, e);
    }
  }

  L get(long id, L layout) throws IOException {
    FileBuffer fb = getChannel(id, false);
    if (fb == null) {
      return null;
    }
    long offset = getOffset(id);
    synchronized (fb) {
      fb.buf.position(0);
      fb.fc.read(fb.buf, offset);
      fb.buf.position(0);
      if (fb.buf.remaining() <= 0) {
        return null;
      }
      layout.write(fb.buf);
    }
    return layout;
  }

  void delete(long id) throws IOException {
    FileBuffer fb = getChannel(id, false);
    if (fb == null) {
      return;
    }
    long offset = getOffset(id);
    try {
      synchronized (fb) {
        ByteBuffer buf = deletedItem.duplicate();
        buf.position(0);
        fb.fc.write(buf, offset);
      }
    } catch (IOException e) {
      LOG.error("Could not write a delete record row to the file. Id: {}, FileChannel: {}", id, fb.fc, e);
    }
  }

  @Override
  public void close() {
    for (FileBuffer fb : pool) {
      close(fb.fc);
    }
  }

  private void close(FileChannel fc) {
    if (fc == null || !fc.isOpen()) {
      return;
    }
    synchronized (fc) {
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

  private static class FileBuffer {
    final FileChannel fc;
    final ByteBuffer buf;
    FileBuffer(FileChannel fc, ByteBuffer buf) {
      this.fc = fc;
      this.buf = buf;
    }
  }
}
