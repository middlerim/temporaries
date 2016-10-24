package com.middlerim.client;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import com.middlerim.location.Coordinate;
import com.middlerim.message.SequentialMessage;

public final class CentralEvents {

  private static final List<Listener<StartedEvent>> startedListeners = new ArrayList<>(2);
  private static final List<Listener<ErrorEvent>> errorListeners = new ArrayList<>(2);
  private static final List<Listener<ReceiveMessageEvent>> receiveMessageListeners = new ArrayList<>(2);
  private static final List<Listener<ReceivedEvent>> receivedListeners = new ArrayList<>(2);
  private static final List<Listener<ReceivedTextEvent>> receivedTextListeners = new ArrayList<>(2);
  private static final List<Listener<LostMessageEvent>> lostMessageListeners = new ArrayList<>(2);
  private static final List<Listener<SendMessageEvent>> sendMessageListeners = new ArrayList<>(2);
  private static final Map<String, Listener<?>> listeneres = new HashMap<>();
  private static AtomicBoolean started = new AtomicBoolean(false);

  public static void removeListener(String name) {
    Listener<?> listener = listeneres.remove(name);
    if (listener == null) {
      throw new IllegalStateException(name + " hasn't been added.");
    }
    startedListeners.remove(listener);
    errorListeners.remove(listener);
    receiveMessageListeners.remove(listener);
    receivedListeners.remove(listener);
    receivedTextListeners.remove(listener);
    lostMessageListeners.remove(listener);
    sendMessageListeners.remove(listener);
  }

  private static <E extends Event> void addListener(String name, Listener<E> listener, List<Listener<E>> ls) {
    if (listeneres.containsKey(name)) {
      removeListener(name);
    }
    listeneres.put(name, listener);
    ls.add(listener);
  }

  public static void onStarted(String name, Listener<StartedEvent> listener) {
    synchronized (started) {
      if (started.get()) {
        listener.handle(new StartedEvent());
      } else {
        addListener(name, listener, startedListeners);
      }
    }
  }

  public static void onError(String name, Listener<ErrorEvent> listener) {
    addListener(name, listener, errorListeners);
  }

  public static void onReceiveMessage(String name, Listener<ReceiveMessageEvent> listener) {
    addListener(name, listener, receiveMessageListeners);
  }

  public static void onReceived(String name, Listener<ReceivedEvent> listener) {
    addListener(name, listener, receivedListeners);
  }

  public static void onReceivedText(String name, Listener<ReceivedTextEvent> listener) {
    addListener(name, listener, receivedTextListeners);
  }

  public static void onLostMessage(String name, Listener<LostMessageEvent> listener) {
    addListener(name, listener, lostMessageListeners);
  }

  public static void onSendMessage(String name, Listener<SendMessageEvent> listener) {
    addListener(name, listener, sendMessageListeners);
  }

  public static interface Listener<EV extends Event> {
    void handle(EV event);
  }

  private interface Event {
  }

  public static class StartedEvent implements Event {
  }

  public static class ErrorEvent implements Event {
    public final String message;
    public final Throwable cause;

    public ErrorEvent(String message, Throwable cause) {
      this.message = message;
      this.cause = cause;
    }
  }

  public static class ReceiveMessageEvent implements Event {
    public final long userId;
    public final Coordinate location;
    public final String displayName;
    public final ByteBuffer message;

    private ReceiveMessageEvent(long userId, Coordinate location, String displayName, ByteBuffer message) {
      this.userId = userId;
      this.location = location;
      this.displayName = displayName;
      this.message = message;
    }
  }

  public static class ReceivedEvent implements Event {
    public final byte clientSequenceNo;

    private ReceivedEvent(byte clientSequenceNo) {
      this.clientSequenceNo = clientSequenceNo;
    }
  }

  public static class ReceivedTextEvent implements Event {
    public final byte clientSequenceNo;
    public final int numberOfDelivery;

    private ReceivedTextEvent(byte clientSequenceNo, int numberOfDelivery) {
      this.clientSequenceNo = clientSequenceNo;
      this.numberOfDelivery = numberOfDelivery;
    }
  }

  public static class LostMessageEvent implements Event {
    public final SequentialMessage message;
    public final LostMessageEvent.Type type;
    public enum Type {
        LIMIT
    }

    private LostMessageEvent(SequentialMessage message, LostMessageEvent.Type type) {
      this.message = message;
      this.type = type;
    }
  }

  public static class SendMessageEvent implements Event {
    public final int tag;
    public final byte clientSequenceNo;
    public final String displayName;
    public final ByteBuffer message;
    private SendMessageEvent(int tag, byte clientSequenceNo, String displayName, ByteBuffer message) {
      this.tag = tag;
      this.clientSequenceNo = clientSequenceNo;
      this.displayName = displayName;
      this.message = message;
    }
  }

  private static <E extends Event> void handleEvent(E event, List<Listener<E>> ls) {
    for (Listener<E> l : ls) {
      l.handle(event);
    }
  }

  public static void fireStarted() {
    synchronized (started) {
      if (started.compareAndSet(false, true)) {
        handleEvent(new StartedEvent(), startedListeners);
      }
    }
  }

  public static void fireError(String errorCode, Throwable cause) {
    handleEvent(new ErrorEvent(errorCode, cause), errorListeners);
  }

  public static void fireReceiveMessage(long userId, Coordinate location, String displayName, ByteBuffer message) {
    handleEvent(new ReceiveMessageEvent(userId, location, displayName, message), receiveMessageListeners);
  }

  public static void fireReceived(byte clientSequenceNo) {
    handleEvent(new ReceivedEvent(clientSequenceNo), receivedListeners);
  }

  public static void fireReceivedText(byte clientSequenceNo, int numberOfDelivery) {
    handleEvent(new ReceivedTextEvent(clientSequenceNo, numberOfDelivery), receivedTextListeners);
  }

  public static void fireLostMessage(SequentialMessage message, LostMessageEvent.Type type) {
    handleEvent(new LostMessageEvent(message, type), lostMessageListeners);
  }

  public static void fireSendMessage(int tag, byte clientSequenceNo, String displayName, ByteBuffer messageBytes) {
    handleEvent(new SendMessageEvent(tag, clientSequenceNo, displayName, messageBytes), sendMessageListeners);
  }
}
