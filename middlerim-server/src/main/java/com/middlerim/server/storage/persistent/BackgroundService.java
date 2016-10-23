package com.middlerim.server.storage.persistent;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BackgroundService<L extends Persistent<L>> implements Runnable, Closeable {
  private ExecutorService threadPool = Executors.newFixedThreadPool(1);

  private static final Logger LOG = LoggerFactory.getLogger(BackgroundService.class);

  private AtomicBoolean start = new AtomicBoolean(false);

  private Segments<L> segments;
  private LinkedBlockingQueue<L> putQueue;
  private LinkedBlockingQueue<Long> deleteQueue;

  private static final List<Closeable> RESOURCES = new ArrayList<>();
  static {
    Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
      @Override
      public void run() {
        closeAll();
      }
    }));
  }

  private static void closeAll() {
    for (int i = RESOURCES.size() - 1; 0 <= i; i--) {
      try {
        RESOURCES.get(i).close();
      } catch (Exception e) {
        e.printStackTrace(System.err);
      }
    }
  }

  BackgroundService(Segments<L> segments) {
    this.segments = segments;
    this.putQueue = new LinkedBlockingQueue<L>();
    this.deleteQueue = new LinkedBlockingQueue<Long>();
    RESOURCES.add(this);
  }

  void put(L item) {
    putQueue.offer(item);
    startIfNot();
  }
  L get(Long id, L layout) {
    try {
      return segments.get(id, layout);
    } catch (Exception e) {
      LOG.error("Caught the unexpected exception.", e);
      return null;
    }
  }
  void delete(Long id) {
    deleteQueue.offer(id);
    startIfNot();
  }

  private void startIfNot() {
    if (start.compareAndSet(false, true)) {
      execute();
    }
  }

  private void execute() {
    if (threadPool.isShutdown()) {
      threadPool = Executors.newFixedThreadPool(1);
    }
    threadPool.execute(this);
  }

  @Override
  public void run() {

    try {
      try {
        L item = null;
        for (;;) {
          if (putQueue.isEmpty()) {
            for (Long deleteId = deleteQueue.peek(); deleteId != null; deleteId = deleteQueue.peek()) {
              segments.delete(deleteId);
              deleteQueue.poll();
            }
          }
          item = putQueue.take();
          segments.put(item);
        }
      } catch (InterruptedException e) {
        if (start.get()) {
          LOG.error("Service is interrupted unexpectedly.", e);
        }
        L item = null;
        while ((item = putQueue.poll()) != null) {
          segments.put(item);
        }
        Long deleteId;
        while ((deleteId = deleteQueue.poll()) != null) {
          segments.delete(deleteId);
        }
      }
    } catch (Exception e) {
      LOG.error("Caught the unexpected exception.", e);
    }
    if (putQueue.isEmpty() && deleteQueue.isEmpty()) {
      return;
    }
    LOG.warn("These item might not be persisted:");
    LOG.warn("    put queue: {}", putQueue);
    LOG.warn(" delete queue: {}", deleteQueue);
  }

  @Override
  public void close() {
    if (start.compareAndSet(true, false)) {
      shutdownAndAwaitTermination();
    }
  }

  void shutdownAndAwaitTermination() {
    if (threadPool.isShutdown()) {
      return;
    }
    try {
      threadPool.shutdownNow();
      if (!threadPool.awaitTermination(3, TimeUnit.SECONDS)) {
        LOG.error("Could not close the background storage service.");
      }
    } catch (InterruptedException ie) {
      threadPool.shutdownNow();
      Thread.currentThread().interrupt();
    }
  }

  static void closeOnShutdown(Closeable resource) {
    RESOURCES.add(0, resource);
  }
}
