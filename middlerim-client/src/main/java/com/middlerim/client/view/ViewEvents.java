package com.middlerim.client.view;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import com.middlerim.location.Point;

public final class ViewEvents {

  private static final List<Listener<CreateEvent>> createListeners = new ArrayList<>(1);
  private static final List<Listener<LocationUpdateEvent>> locationUpdateListeners = new ArrayList<>(1);
  private static final List<Listener<SubmitMessageEvent>> submitMessageListeners = new ArrayList<>(1);
  private static final List<Listener<DestroyEvent>> destroyListeners = new ArrayList<>(1);
  private static final List<Listener<StatusChangeEvent>> statusChangeListeners = new ArrayList<>(2);

  public static void removeListener(Listener<? extends Event> listener) {
    createListeners.remove(listener);
    locationUpdateListeners.remove(listener);
    submitMessageListeners.remove(listener);
    destroyListeners.remove(listener);
    statusChangeListeners.remove(listener);
  }

  private static <E extends Event> void addListener(Listener<E> listener, List<Listener<E>> ls) {
    if (ls.contains(listener)) {
      throw new IllegalStateException();
    }
    ls.add(listener);
  }

  public static void onCreate(Listener<CreateEvent> listener) {
    addListener(listener, createListeners);
  }

  public static void onLocationUpdate(Listener<LocationUpdateEvent> listener) {
    addListener(listener, locationUpdateListeners);
  }

  public static void onSubmitMessage(Listener<SubmitMessageEvent> listener) {
    addListener(listener, submitMessageListeners);
  }

  public static void onDestroy(Listener<DestroyEvent> listener) {
    addListener(listener, destroyListeners);
  }

  public static void onStatusChange(Listener<StatusChangeEvent> listener) {
    addListener(listener, statusChangeListeners);
  }

  public interface Listener<EV extends Event> {
    void handle(EV event);
  }

  private interface Event {
  }

  public static class CreateEvent implements Event {
  }

  public static class LocationUpdateEvent implements Event {
    public final Point location;

    private LocationUpdateEvent(Point location) {
      this.location = location;
    }
  }

  public static class SubmitMessageEvent implements Event {
    public final ByteBuffer message;
    public final byte messageCommand;

    private SubmitMessageEvent(ByteBuffer message, byte messageCommand) {
      this.message = message;
      this.messageCommand = messageCommand;
    }
  }

  public static class DestroyEvent implements Event {
  }

  public static class StatusChangeEvent implements Event {
    public final int statusCode;
    private StatusChangeEvent(int statusCode) {
      this.statusCode = statusCode;
    }
  }

  private static <E extends Event> void handleEvent(E event, List<Listener<E>> ls) {
    for (Listener<E> l : ls) {
      l.handle(event);
    }
  }

  public static void fireCreate() {
    handleEvent(new CreateEvent(), createListeners);
  }

  public static void fireLocationUpdate(Point location) {
    handleEvent(new LocationUpdateEvent(location), locationUpdateListeners);
  }

  public static void fireSubmitMessage(ByteBuffer message, byte messageCommand) {
    handleEvent(new SubmitMessageEvent(message, messageCommand), submitMessageListeners);
  }

  public static void fireDestroy() {
    handleEvent(new DestroyEvent(), destroyListeners);
  }

  public static void fireStatusChange(int statusCode) {
    handleEvent(new StatusChangeEvent(statusCode), statusChangeListeners);
  }
}
