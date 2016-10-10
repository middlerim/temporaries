package com.middlerim.server.storage.persistent;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BackgroundService<L extends Persistent> implements Runnable {
  private ExecutorService threadPool = Executors.newFixedThreadPool(1);

  private static final Logger LOG = LoggerFactory.getLogger(BackgroundService.class);

  private AtomicBoolean start = new AtomicBoolean(false);

  private Segments<L> segments;
  private LinkedBlockingQueue<L> putQueue;
  private LinkedBlockingQueue<Long> deleteQueue;

  BackgroundService(Segments<L> segments) {
    this.segments = segments;
    this.putQueue = new LinkedBlockingQueue<L>();
    this.deleteQueue = new LinkedBlockingQueue<Long>();
  }

  void put(L item) {
    putQueue.offer(item);
    startIfNot();
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

    L item = null;
    try {
      for (;;) {
        if (putQueue.isEmpty()) {
          for (Long deleteId = deleteQueue.peek(); deleteId != null; deleteId = deleteQueue.peek()) {
            if (deleteId != null) {
              segments.delete(deleteId);
            }
          }
        }
        item = putQueue.take();
        segments.put(item);
      }
    } catch (InterruptedException e) {
      // ignore the error and rerun.
      run();
    } catch (Exception e) {
      LOG.error("Caught the unexpected exception. Rebooting...", e);
      LOG.warn("The item might not be persisted: {}", item);
      close();
      run();
      LOG.info("Success rebooting the background service.");
    }
  }

  void close() {
    start.set(false);
    if (!threadPool.isShutdown()) {
      shutdownAndAwaitTermination(threadPool);
    }
  }

  void shutdownAndAwaitTermination(ExecutorService pool) {
    pool.shutdown();
    try {
      if (!pool.awaitTermination(60, TimeUnit.SECONDS)) {
        LOG.warn("Could have not closed the background storage service for 60 sec. Closing forcefully.");
        pool.shutdownNow();
        if (!pool.awaitTermination(60, TimeUnit.SECONDS))
          LOG.error("Could not close the background storage service.");
      }
    } catch (InterruptedException ie) {
      pool.shutdownNow();
      Thread.currentThread().interrupt();
    }
  }
}
