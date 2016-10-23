package com.middlerim.client.view;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import com.middlerim.client.CentralEvents;
import com.middlerim.client.session.Sessions;
import com.middlerim.location.Coordinate;
import com.middlerim.session.Session;

public class MessagePool<T> {
  public interface Adapter<T> {
    T onReceive(long userId, Coordinate location, String displayName, ByteBuffer message, int numberOfDelivery);
  }

  public interface RemovedListener<T> {
    void onRemoved(int index, T message);
  }

  private Map<Byte, CentralEvents.SendMessageEvent> myMessages;

  private CentralEvents.Listener<CentralEvents.ReceivedTextEvent> receivedTextListener = new CentralEvents.Listener<CentralEvents.ReceivedTextEvent>() {
    @Override
    public void handle(CentralEvents.ReceivedTextEvent event) {
      CentralEvents.SendMessageEvent sendMessageEvent = myMessages.remove(event.clientSequenceNo);
      if (sendMessageEvent != null) {
        Session session = Sessions.getSession();
        onReceiveMessage(session.sessionId.userId(), null, sendMessageEvent.displayName, sendMessageEvent.message, event.numberOfDelivery);
      }
    }
  };

  private CentralEvents.Listener<CentralEvents.ReceiveMessageEvent> receiveMessageListener = new CentralEvents.Listener<CentralEvents.ReceiveMessageEvent>() {
    @Override
    public void handle(CentralEvents.ReceiveMessageEvent event) {
      onReceiveMessage(event.userId, event.location, event.displayName, event.message, -1);
    }
  };

  private CentralEvents.Listener<CentralEvents.SendMessageEvent> sendMessageListener = new CentralEvents.Listener<CentralEvents.SendMessageEvent>() {

    @Override
    public void handle(CentralEvents.SendMessageEvent event) {
      myMessages.put(event.clientSequenceNo, event);
    }
  };

  private Object[] pool;

  private Adapter<T> aapter;
  private RemovedListener<T> removedListener;
  private int size;
  private int last;
  private final int capacity;
  private boolean listening;
  public MessagePool(int capacity, Adapter<T> aapter) {
    this.capacity = capacity;
    this.aapter = aapter;
    this.pool = new Object[capacity];
    this.myMessages = new HashMap<>();
    startListen();
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
  private void addLast(T elem) {
    if (last >= capacity) {
      last = 0;
    }
    Object old = pool[last];
    if (old != null && removedListener != null) {
      removedListener.onRemoved(size - capacity, (T) old);
    }
    size++;
    pool[last++] = elem;
  }

  public int capacity() {
    return capacity;
  }
  public int size() {
    return size;
  }

  private void onReceiveMessage(long userId, Coordinate location, String displayName, ByteBuffer message, int numberOfDelivery) {
    addLast(aapter.onReceive(userId, location, displayName, message, numberOfDelivery));
  }

  public void onRemoved(RemovedListener<T> listener) {
    this.removedListener = listener;
  }

  public void startListen() {
    if (listening) {
      return;
    }
    listening = true;
    CentralEvents.onReceivedText(receivedTextListener);
    CentralEvents.onReceiveMessage(receiveMessageListener);
    CentralEvents.onSendMessage(sendMessageListener);
  }

  public void stopListen() {
    listening = false;
    CentralEvents.removeListener(receivedTextListener);
    CentralEvents.removeListener(receiveMessageListener);
    CentralEvents.removeListener(sendMessageListener);
  }
}
