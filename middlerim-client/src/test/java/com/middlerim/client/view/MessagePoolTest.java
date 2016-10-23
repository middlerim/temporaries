package com.middlerim.client.view;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.junit.Test;

import com.middlerim.client.CentralEvents;
import com.middlerim.location.Coordinate;

public class MessagePoolTest {

  private static class Entry {
    long userId;
    Coordinate location;
    String displayName;
    ByteBuffer message;
    int numberOfDelivery;

    @Override
    public boolean equals(Object obj) {
      Entry b = (Entry) obj;
      return userId == b.userId && location.equals(b.location) && displayName.equals(b.displayName) && message.equals(b.message) && numberOfDelivery == b.numberOfDelivery;
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

  @Test
  public void testEmptyPool() {
    MessagePool<Entry> pool = new MessagePool<>(1, messagePoolAdapter);
    Entry entry = pool.get(0);
    assertNull(entry);
  }

  @Test
  public void testOnReceiveNewMessage() {
    int capacity = 3;
    MessagePool<Entry> pool = new MessagePool<>(capacity, messagePoolAdapter);
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
    MessagePool<Entry> pool = new MessagePool<>(capacity, messagePoolAdapter);
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
}
