package com.middlerim.client.view;

import java.io.Closeable;
import java.io.File;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.middlerim.client.Config;
import com.middlerim.client.central.CentralEvents;
import com.middlerim.client.session.Sessions;
import com.middlerim.location.Coordinate;
import com.middlerim.location.Point;
import com.middlerim.session.Session;
import com.middlerim.session.SessionId;
import com.middlerim.storage.persistent.FixedLayoutPersistentStorage;
import com.middlerim.storage.persistent.Persistent;
import com.middlerim.storage.persistent.FixedLayoutStorageInformation;

public class MessagePool<T> implements Closeable {
  private static final Logger LOG = LoggerFactory.getLogger(Config.INTERNAL_APP_NAME);

  public interface Adapter<T> {
    T onReceive(long userId, Coordinate location, String displayName, byte[] message, int numberOfDelivery);
    File storage();
  }

  public interface AddedListener<T> {
    void onAdded(int index, T message);
  }

  public interface RemovedListener<T> {
    void onRemoved(int index, T message);
  }

  private Map<Integer, ViewEvents.SubmitMessageEvent> myMessages;

  private CentralEvents.Listener<CentralEvents.ReceivedTextEvent> receivedTextListener = new CentralEvents.Listener<CentralEvents.ReceivedTextEvent>() {
    @Override
    public void handle(CentralEvents.ReceivedTextEvent event) {
      ViewEvents.SubmitMessageEvent submitMessageEvent = myMessages.remove(event.tag);
      if (submitMessageEvent != null) {
        Session session = Sessions.getSession();
        onReceiveMessage(session.sessionId.userId(), null, submitMessageEvent.displayName, submitMessageEvent.message, event.numberOfDelivery);
      }
    }
  };

  private CentralEvents.Listener<CentralEvents.ReceiveMessageEvent> receiveMessageListener = new CentralEvents.Listener<CentralEvents.ReceiveMessageEvent>() {
    @Override
    public void handle(CentralEvents.ReceiveMessageEvent event) {
      byte[] message = new byte[event.message.readableBytes()];
      event.message.readBytes(message);
      onReceiveMessage(event.userId, event.location, event.displayName, message, -1);
    }
  };

  private ViewEvents.Listener<ViewEvents.SubmitMessageEvent> submitMessageListener = new ViewEvents.Listener<ViewEvents.SubmitMessageEvent>() {

    @Override
    public void handle(ViewEvents.SubmitMessageEvent event) {
      myMessages.put(event.tag, event);
    }
  };

  private Object[] pool;

  private Adapter<T> adapter;
  private List<RemovedListener<T>> removedListeners = new ArrayList<>(2);
  private List<AddedListener<T>> addedListeners = new ArrayList<>(2);
  private int size;
  private int last;
  private final int capacity;
  private boolean listening;
  private final FixedLayoutPersistentStorage<Message> storage;
  public MessagePool(int capacity, Adapter<T> adapter) {
    this.capacity = capacity;
    this.adapter = adapter;
    this.pool = new Object[capacity];
    this.myMessages = new HashMap<>();
    this.storage = new FixedLayoutPersistentStorage<Message>(
        new FixedLayoutStorageInformation<Message>("msgpool", Config.MAX_TEXT_BYTES, Config.MAX_TEXT_BYTES * capacity, Config.MAX_TEXT_BYTES * capacity) {
          @Override
          public File storage() {
            return MessagePool.this.adapter.storage();
          }
        });
  }

  public MessagePool(Adapter<T> aaptor) {
    this(100, aaptor);
  }

  @SuppressWarnings("unchecked")
  public T get(int i) {
    if (size - capacity > i) {
      return null;
    }
    int realIndex = i % capacity;
    return (T) pool[realIndex];
  }

  @SuppressWarnings("unchecked")
  public T getLatest(int i) {
    return (T) pool[i];
  }

  public ViewEvents.SubmitMessageEvent getUnreachedMessage(int tag) {
    return myMessages.get(tag);
  }

  @SuppressWarnings("unchecked")
  private void addLast(T elem) {
    if (last >= capacity) {
      last = 0;
    }
    Object old = pool[last];
    if (old != null) {
      for (RemovedListener<T> l : removedListeners) {
        l.onRemoved(size - capacity, (T) old);
      }
    }
    size++;
    pool[last++] = elem;
    for (AddedListener<T> l : addedListeners) {
      l.onAdded(size - 1, (T) elem);
    }
  }

  public int capacity() {
    return capacity;
  }
  public int size() {
    return size;
  }

  private void onReceiveMessage(long userId, Coordinate location, String displayName, byte[] message, int numberOfDelivery) {
    storage.put(new Message(last, userId, location, displayName, message, numberOfDelivery));
    addLast(adapter.onReceive(userId, location, displayName, message, numberOfDelivery));
  }

  public void onAdded(AddedListener<T> listener) {
    this.addedListeners.add(listener);
  }

  public void onRemoved(RemovedListener<T> listener) {
    this.removedListeners.add(listener);
  }

  public MessagePool<T> startListen() {
    if (listening) {
      return this;
    }
    listening = true;
    CentralEvents.onReceivedText("MessagePool.receivedTextListener", receivedTextListener);
    CentralEvents.onReceiveMessage("MessagePool.receiveMessageListener", receiveMessageListener);
    ViewEvents.onSubmitMessage("MessagePool.submitMessageListener", submitMessageListener);
    return this;
  }

  @Override
  public void close() {
    storage.close();
  }

  public MessagePool<T> stopListen() {
    listening = false;
    CentralEvents.removeListener("MessagePool.receivedTextListener");
    CentralEvents.removeListener("MessagePool.receiveMessageListener");
    CentralEvents.removeListener("MessagePool.sendMessageListener");
    return this;
  }

  public boolean loadLatestMessages(int size) {
    int begin = size < capacity ? 0 : size % capacity;
    int end = size < capacity ? size : begin + capacity;
    for (int i = begin; i < end; i++) {
      int id = i > capacity ? i - capacity : i;
      Message message = new Message(id);
      message = storage.get(message.index, message);
      if (message.userId == SessionId.UNASSIGNED_USERID) {
        LOG.warn("Could not read a message cache from file storage.");
        return false;
      }
      addLast(adapter.onReceive(message.userId, message.location, new String(message.displayName, Config.MESSAGE_ENCODING), message.message, message.numberOfDelivery));
    }
    return true;
  }

  private static class Message implements Persistent<Message> {
    private final long index;
    private long userId;
    private Coordinate location;
    private byte[] displayName;
    private byte[] message;
    private int numberOfDelivery;
    Message(long index) {
      this.index = index;
    }
    Message(long index, long userId, Coordinate location, String displayName, byte[] message, int numberOfDelivery) {
      this.index = index;
      this.userId = userId;
      this.location = location;
      this.displayName = displayName.getBytes(Config.MESSAGE_ENCODING);
      this.message = message;
      this.numberOfDelivery = numberOfDelivery;
    }

    @Override
    public long id() {
      return index;
    }

    @Override
    public void read(ByteBuffer buf) {
      buf.putLong(userId);
      if (location != null) {
        Point point = location.toPoint();
        buf
            .putInt(point.latitude)
            .putInt(point.longitude);
      } else {
        buf
            .putInt(0)
            .putInt(0);
      }
      buf
          .putInt(numberOfDelivery)
          .putInt(displayName.length)
          .put(displayName)
          .putInt(message.length)
          .put(message);
    }

    @Override
    public void write(ByteBuffer buf) {
      this.userId = buf.getLong();
      this.location = new Point(buf.getInt(), buf.getInt()).toCoordinate();
      this.numberOfDelivery = buf.getInt();
      this.displayName = new byte[buf.getInt()];
      buf.get(this.displayName);
      this.message = new byte[buf.getInt()];
      buf.get(this.message);
    }
  }
}
