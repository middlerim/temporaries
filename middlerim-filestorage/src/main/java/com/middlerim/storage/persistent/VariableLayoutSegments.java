package com.middlerim.storage.persistent;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class VariableLayoutSegments<L extends VariableLayout> implements Closeable {

  private static final Logger LOG = LoggerFactory.getLogger(VariableLayoutSegments.class);

  private final VariableLayoutStorageInformation info;
  private final long segmentSize;
  private final FileBuffer[] pool;

  private final Map<Long, Meta> offsetMap = new ConcurrentHashMap<>();
  private long last;
  private byte[] deleteMark = new byte[]{0b0};

  VariableLayoutSegments(VariableLayoutStorageInformation info) {
    BackgroundService.closeOnShutdown(this);
    this.info = info;
    if (info.maxStorageSize() > info.segmentSize()) {
      this.segmentSize = info.segmentSize();
    } else {
      this.segmentSize = info.maxStorageSize();
    }
    LOG.info("Created segments for {}: a segment size: {} byte, max storage size: {} byte", info.storageId(), segmentSize, info.maxStorageSize());
    this.pool = new FileBuffer[(int) (Math.floor((double) info.maxStorageSize() / this.segmentSize)) + 1];
    LOG.info("                max file channel size: {}", pool.length);
  }

  private int channelIndex(long offset) {
    return (int) (segmentSize / offset);
  }

  private long getVirtualOffset(long offset) {
    return offset % segmentSize;
  }

  private FileBuffer getChannel(long offset, boolean createIfNot) throws IOException {
    int chIndex = channelIndex(offset);
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
      fb = new FileBuffer(f.getChannel());
      pool[chIndex] = fb;
    } else if (!fb.fc.isOpen()) {
      pool[chIndex] = null;
      return getChannel(offset, createIfNot);
    }
    return fb;
  }

  void put(L layout) throws IOException {
    long id = layout.id();
    Meta meta = offsetMap.get(id);
    if (meta != null) {
      throw new IllegalStateException("Offset " + meta.offset + " has already active data.");
    } else {
      meta = new Meta(last, layout.length());
    }
    FileBuffer fb = getChannel(meta.offset, true);
    long vOffset = getVirtualOffset(meta.offset);
    try {
      synchronized (fb) {
        fb.fc.write(layout.read(), vOffset);
      }
    } catch (IOException e) {
      LOG.error("Could not write to the file. Data: {}, FileChannel: {}", layout, fb.fc, e);
    }
  }

  L get(long id, L layout) throws IOException {
    Meta meta = offsetMap.get(id);
    if (meta == null) {
      return null;
    }
    FileBuffer fb = getChannel(meta.offset, false);
    if (fb == null) {
      deleteMeta(meta);
      return null;
    }
    long vOffset = getVirtualOffset(meta.offset);
    synchronized (fb) {
      ByteBuffer buf = ByteBuffer.allocateDirect(meta.length);
      fb.fc.read(buf, vOffset);
      buf.position(0);
      if (buf.remaining() <= 0 || buf.get() == deleteMark[0]) {
        return null;
      }
      layout.write(buf);
    }
    return layout;
  }

  void delete(long id) throws IOException {
    Meta meta = offsetMap.get(id);
    if (meta == null) {
      return;
    }
    try {
      FileBuffer fb = getChannel(meta.offset, false);
      if (fb == null) {
        return;
      }
      long vOffset = getVirtualOffset(meta.offset);
      try {
        synchronized (fb) {
          fb.fc.write(ByteBuffer.wrap(deleteMark), vOffset);
        }
      } catch (IOException e) {
        LOG.error("Could not write a delete record row to the file. Id: {}, FileChannel: {}", id, fb.fc, e);
      }
    } finally {
      deleteMeta(meta);
    }
  }

  private void deleteMeta(Meta meta) {
    offsetMap.remove(meta);
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
    FileBuffer(FileChannel fc) {
      this.fc = fc;
    }
  }

  private static class Meta {
    final long offset;
    final int length;
    Meta(long offset, int length) {
      this.offset = offset;
      this.length = length;
    }
  }
}
