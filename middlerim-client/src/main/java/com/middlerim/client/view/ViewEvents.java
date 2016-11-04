package com.middlerim.client.view;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import com.middlerim.location.Coordinate;

public final class ViewEvents {

  private static final List<Listener<CreateEvent>> createListeners = new ArrayList<>(2);
  private static final List<Listener<LocationUpdateEvent>> locationUpdateListeners = new ArrayList<>(2);
  private static final List<Listener<SubmitMessageEvent>> submitMessageListeners = new ArrayList<>(2);
  private static final List<Listener<ChangeAreaEvent>> changeAreaListeners = new ArrayList<>(2);
  private static final List<Listener<ResumeEvent>> resumeListeners = new ArrayList<>(2);
  private static final List<Listener<PauseEvent>> pauseListeners = new ArrayList<>(2);
  private static final List<Listener<DestroyEvent>> destroyListeners = new ArrayList<>(2);
  private static final List<Listener<StatusChangeEvent>> statusChangeListeners = new ArrayList<>(2);
  private static final Map<String, Listener<?>> listeneres = new HashMap<>();
  private static AtomicBoolean started = new AtomicBoolean(false);

  public static void removeListener(String name) {
    Listener<?> listener = listeneres.remove(name);
    if (listener == null) {
      return;
    }
    createListeners.remove(listener);
    locationUpdateListeners.remove(listener);
    submitMessageListeners.remove(listener);
    changeAreaListeners.remove(listener);
    resumeListeners.remove(listener);
    pauseListeners.remove(listener);
    destroyListeners.remove(listener);
    statusChangeListeners.remove(listener);
  }

  private static <E extends Event> void addListener(String name, Listener<E> listener, List<Listener<E>> ls) {
    if (listeneres.containsKey(name)) {
      removeListener(name);
    }
    listeneres.put(name, listener);
    ls.add(listener);
  }

  public static void onCreate(String name, Listener<CreateEvent> listener) {
    addListener(name, listener, createListeners);
  }

  public static void onLocationUpdate(String name, Listener<LocationUpdateEvent> listener) {
    addListener(name, listener, locationUpdateListeners);
  }

  public static void onSubmitMessage(String name, Listener<SubmitMessageEvent> listener) {
    addListener(name, listener, submitMessageListeners);
  }

  public static void onChangeArea(String name, Listener<ChangeAreaEvent> listener) {
    addListener(name, listener, changeAreaListeners);
  }

  public static void onResume(String name, Listener<ResumeEvent> listener) {
    addListener(name, listener, resumeListeners);
    if (started != null && started.get()) {
      listener.handle(new ResumeEvent());
    }
  }

  public static void onPause(String name, Listener<PauseEvent> listener) {
    addListener(name, listener, pauseListeners);
  }

  public static void onDestroy(String name, Listener<DestroyEvent> listener) {
    addListener(name, listener, destroyListeners);
  }

  public static void onStatusChange(String name, Listener<StatusChangeEvent> listener) {
    addListener(name, listener, statusChangeListeners);
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
    public final int tag;
    public final String displayName;
    public final byte messageCommand;
    public final ByteBuffer message;

    private SubmitMessageEvent(int tag, String displayName, byte messageCommand, ByteBuffer message) {
      this.tag = tag;
      this.displayName = displayName;
      this.messageCommand = messageCommand;
      this.message = message;
    }
  }

  public static class ChangeAreaEvent implements Event {
    public final int radiusMeter;
    private ChangeAreaEvent(int radiusMeter) {
      this.radiusMeter = radiusMeter;
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

  public static void fireSubmitMessage(int tag, String displayName, byte messageCommand, ByteBuffer message) {
    handleEvent(new SubmitMessageEvent(tag, displayName, messageCommand, message), submitMessageListeners);
  }

  public static void fireChangeArea(int radiusMeter) {
    handleEvent(new ChangeAreaEvent(radiusMeter), changeAreaListeners);
  }

  public static void fireResume() {
    handleEvent(new ResumeEvent(), resumeListeners);
    started.set(true);
  }

  public static void firePause() {
    handleEvent(new PauseEvent(), pauseListeners);
    started.set(false);
  }

  public static void fireDestroy() {
    handleEvent(new DestroyEvent(), destroyListeners);
  }

  public static void fireStatusChange(int statusCode) {
    handleEvent(new StatusChangeEvent(statusCode), statusChangeListeners);
  }
}
