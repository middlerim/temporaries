package com.middlerim.client;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public final class CentralEvents {

  private static final List<Listener<StartedEvent>> startedListeners = new ArrayList<>(1);
  private static final List<Listener<ErrorEvent>> errorListeners = new ArrayList<>(1);
  private static final List<Listener<ReceiveMessageEvent>> receiveMessageListeners = new ArrayList<>(1);
  private static final List<Listener<ReceivedEvent>> receivedListeners = new ArrayList<>(1);
  private static final List<Listener<LostMessageEvent>> lostMessageListeners = new ArrayList<>(1);

  public static void removeListener(Listener<? extends Event> listener) {
    startedListeners.remove(listener);
    errorListeners.remove(listener);
    receiveMessageListeners.remove(listener);
    receivedListeners.remove(listener);
    lostMessageListeners.remove(listener);
  }

  private static <E extends Event> void addListener(Listener<E> listener, List<Listener<E>> ls) {
    if (ls.contains(listener)) {
      throw new IllegalStateException();
    }
    ls.add(listener);
  }

  public static void onStarted(Listener<StartedEvent> listener) {
    addListener(listener, startedListeners);
  }

  public static void onError(Listener<ErrorEvent> listener) {
    addListener(listener, errorListeners);
  }

  public static void onReceiveMessage(Listener<ReceiveMessageEvent> listener) {
    addListener(listener, receiveMessageListeners);
  }

  public static void onReceived(Listener<ReceivedEvent> listener) {
    addListener(listener, receivedListeners);
  }

  public static void onLostMessage(Listener<LostMessageEvent> listener) {
    addListener(listener, lostMessageListeners);
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
    public final ByteBuffer message;

    private ReceiveMessageEvent(ByteBuffer message) {
      this.message = message;
    }
  }

  public static class ReceivedEvent implements Event {
    public final short sequenceNo;

    private ReceivedEvent(short sequenceNo) {
      this.sequenceNo = sequenceNo;
    }
  }

  public static class LostMessageEvent implements Event {
    public final short sequenceNo;

    private LostMessageEvent(short sequenceNo) {
      this.sequenceNo = sequenceNo;
    }
  }

  private static <E extends Event> void handleEvent(E event, List<Listener<E>> ls) {
    for (Listener<E> l : ls) {
      l.handle(event);
    }
  }

  public static void fireStarted() {
    handleEvent(new StartedEvent(), startedListeners);
  }

  public static void fireError(String errorCode, Throwable cause) {
    handleEvent(new ErrorEvent(errorCode, cause), errorListeners);
  }

  public static void fireReceiveMessage(ByteBuffer message) {
    handleEvent(new ReceiveMessageEvent(message), receiveMessageListeners);
  }

  public static void fireReceived(short sequenceNo) {
    handleEvent(new ReceivedEvent(sequenceNo), receivedListeners);
  }

  public static void fireLostMessage(short sequenceNo) {
    handleEvent(new LostMessageEvent(sequenceNo), lostMessageListeners);
  }
}
