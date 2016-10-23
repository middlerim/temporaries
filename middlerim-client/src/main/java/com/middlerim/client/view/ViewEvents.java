package com.middlerim.client.view;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import com.middlerim.location.Coordinate;

public final class ViewEvents {

  private static final List<Listener<CreateEvent>> createListeners = new ArrayList<>(1);
  private static final List<Listener<LocationUpdateEvent>> locationUpdateListeners = new ArrayList<>(1);
  private static final List<Listener<SubmitMessageEvent>> submitMessageListeners = new ArrayList<>(1);
  private static final List<Listener<ResumeEvent>> resumeListeners = new ArrayList<>(1);
  private static final List<Listener<PauseEvent>> pauseListeners = new ArrayList<>(1);
  private static final List<Listener<DestroyEvent>> destroyListeners = new ArrayList<>(1);
  private static final List<Listener<StatusChangeEvent>> statusChangeListeners = new ArrayList<>(2);

  public static void removeListener(Listener<? extends Event> listener) {
    createListeners.remove(listener);
    locationUpdateListeners.remove(listener);
    submitMessageListeners.remove(listener);
    resumeListeners.remove(listener);
    pauseListeners.remove(listener);
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

  public static void onResume(Listener<ResumeEvent> listener) {
    addListener(listener, resumeListeners);
  }

  public static void onPause(Listener<PauseEvent> listener) {
    addListener(listener, pauseListeners);
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
    public final Coordinate location;

    private LocationUpdateEvent(Coordinate location) {
      this.location = location;
    }
  }

  public static class SubmitMessageEvent implements Event {
    public final String displayName;
    public final byte messageCommand;
    public final ByteBuffer message;

    private SubmitMessageEvent(String displayName, byte messageCommand, ByteBuffer message) {
      this.displayName = displayName;
      this.messageCommand = messageCommand;
      this.message = message;
    }
  }

  public static class ResumeEvent implements Event {
  }

  public static class PauseEvent implements Event {
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

  public static void fireLocationUpdate(Coordinate location) {
    handleEvent(new LocationUpdateEvent(location), locationUpdateListeners);
  }

  public static void fireSubmitMessage(String displayName, byte messageCommand, ByteBuffer message) {
    handleEvent(new SubmitMessageEvent(displayName, messageCommand, message), submitMessageListeners);
  }

  public static void fireResume() {
    handleEvent(new ResumeEvent(), resumeListeners);
  }

  public static void firePause() {
    handleEvent(new PauseEvent(), pauseListeners);
  }

  public static void fireDestroy() {
    handleEvent(new DestroyEvent(), destroyListeners);
  }

  public static void fireStatusChange(int statusCode) {
    handleEvent(new StatusChangeEvent(statusCode), statusChangeListeners);
  }
}
