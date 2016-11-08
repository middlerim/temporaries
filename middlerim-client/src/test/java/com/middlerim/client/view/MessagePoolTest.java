package com.middlerim.client.view;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.junit.Before;
import org.junit.Test;

import com.middlerim.client.central.CentralEvents;
import com.middlerim.location.Coordinate;

public class MessagePoolTest {

  private static final File tmpDir = Paths.get("./", "db_test").toFile();

  private static class Entry {
    long userId;
    Coordinate location;
    String displayName;
    ByteBuffer message;
    int numberOfDelivery;

    @Override
    public boolean equals(Object obj) {
      Entry b = (Entry) obj;
      return userId == b.userId
          && location.equals(b.location)
          && displayName.equals(b.displayName)
          && message.equals(b.message)
          && numberOfDelivery == b.numberOfDelivery;
    }

    @Override
    public String toString() {
      return "userId: " + userId + ", location: " + location + ", " + displayName;
    }
  }

  private MessagePool.Adapter<Entry> messagePoolAdapter = new MessagePool.Adapter<Entry>() {
    @Override
    public Entry onReceive(long userId, Coordinate location, String displayName, ByteBuffer message, int numberOfDelivery) {
      Entry entry = new Entry();
      entry.userId = userId;
      entry.location = location;
      entry.displayName = displayName;
      entry.message = message;
      entry.numberOfDelivery = numberOfDelivery;
      return entry;
    }
    @Override
    public File storage() {
      return tmpDir;
    }
  };

  private Random r = new Random();
  private Entry fireReceiveMessage() {
    Entry entry = new Entry();
    entry.userId = r.nextLong();
    entry.location = new Coordinate(r.nextDouble() * r.nextInt(90), r.nextDouble() * r.nextInt(180));
    entry.displayName = "NoName" + r.nextLong();
    entry.message = ByteBuffer.allocate(2);
    entry.message.putChar('あ');
    entry.numberOfDelivery = -1;
    CentralEvents.fireReceiveMessage(entry.userId, entry.location, entry.displayName, entry.message);
    return entry;
  }

  @Before
  public void before() throws IOException {
    for (String s : tmpDir.list()) {
      File currentFile = new File(tmpDir, s);
      currentFile.delete();
    }
  }

  @Test
  public void testEmptyPool() {
    MessagePool<Entry> pool = new MessagePool<>(1, messagePoolAdapter).startListen();
    Entry entry = pool.get(0);
    assertNull(entry);
  }

  @Test
  public void testOnReceiveNewMessage() {
    int capacity = 3;
    MessagePool<Entry> pool = new MessagePool<>(capacity, messagePoolAdapter).startListen();
    for (int i = 0; i < capacity * 3; i++) {
      Entry expect = fireReceiveMessage();
      Entry entry = pool.get(i);
      assertThat(entry, is(expect));
    }

    assertThat(pool.size(), is(capacity * 3));

    assertNull(pool.get(0));
    assertNull(pool.get(pool.size() - capacity - 1));
    assertNotNull(pool.get(pool.size() - capacity));
  }

  int lastRemovedIndex = -1;
  Entry lastRemovedEntry;
  @Test
  public void testOnRemove() {
    int capacity = 3;
    MessagePool<Entry> pool = new MessagePool<>(capacity, messagePoolAdapter).startListen();
    pool.onRemoved(new MessagePool.RemovedListener<Entry>() {
      @Override
      public void onRemoved(int index, Entry entry) {
        lastRemovedIndex = index;
        lastRemovedEntry = entry;
      }
    });
    List<Entry> added = new ArrayList<Entry>();
    // 0 to capacity
    for (int i = 0; i < capacity; i++) {
      added.add(fireReceiveMessage());
      assertThat(lastRemovedIndex, is(-1));
      assertNull(lastRemovedEntry);
    }
    // capacity to capacity * 2
    for (int i = 0; i < capacity; i++) {
      added.add(fireReceiveMessage());
      assertThat(lastRemovedIndex, is(i));
      assertThat(lastRemovedEntry, is(added.get(i)));
    }
    // capacity *2 to capacity * 3
    for (int i = capacity; i < capacity * 2; i++) {
      added.add(fireReceiveMessage());
      assertThat(lastRemovedIndex, is(i));
      assertThat(lastRemovedEntry, is(added.get(i)));
    }
  }

  @Test
  public void testReadOldMessage_samePool() throws InterruptedException {
    int capacity = 2;
    MessagePool<Entry> pool = new MessagePool<>(capacity, messagePoolAdapter).startListen();
    List<Entry> expects = new ArrayList<>();
    for (int i = 0; i < capacity; i++) {
      expects.add(fireReceiveMessage());
    }

    Thread.sleep(100);
    pool.loadLatestMessages(2);
    for (int i = 0; i < capacity; i++) {
      Entry entry = pool.get(i + capacity);
      assertThat(entry.userId, is(expects.get(i).userId));
    }
  }

  @Test
  public void testReadOldMessage_newPool_full() throws InterruptedException {
    int capacity = 3;
    MessagePool<Entry> pool = new MessagePool<>(capacity, messagePoolAdapter).startListen();
    List<Entry> expects = new ArrayList<>();
    for (int i = 0; i < 5; i++) {
      expects.add(fireReceiveMessage());
    }

    Thread.sleep(100);
    int size = pool.size();
    pool = new MessagePool<>(capacity, messagePoolAdapter).startListen();
    pool.loadLatestMessages(size);
    assertThat(pool.size(), is(3));
    for (int i = 0; i < capacity; i++) {
      Entry entry = pool.get(i);
      assertThat(entry.userId, is(expects.get(i + 2).userId));
    }
  }

  @Test
  public void testReadOldMessage_newPool() throws InterruptedException {
    int capacity = 3;
    MessagePool<Entry> pool = new MessagePool<>(capacity, messagePoolAdapter).startListen();
    List<Entry> expects = new ArrayList<>();
    for (int i = 0; i < 2; i++) {
      expects.add(fireReceiveMessage());
    }

    Thread.sleep(100);
    int size = pool.size();
    pool = new MessagePool<>(capacity, messagePoolAdapter).startListen();
    pool.loadLatestMessages(size);
    assertThat(pool.size(), is(2));
    for (int i = 0; i < 2; i++) {
      Entry entry = pool.get(i);
      assertThat("" + i, entry.userId, is(expects.get(i).userId));
    }
  }
}
