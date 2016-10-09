package com.middlerim.server.storage;

import java.util.List;

import com.middlerim.location.Point;
import com.middlerim.server.storage.location.LocationStorage;
import com.middlerim.server.storage.location.LocationStorage.Entry;
import com.middlerim.server.storage.location.SphericalPoint;
import com.middlerim.server.storage.location.VpTree;
import com.middlerim.session.Session;
import com.middlerim.session.SessionId;
import com.middlerim.session.SessionListener;

public final class Locations {

  private static LocationStorage<SphericalPoint> map = new VpTree<>();

  public static void updateLocation(Session session, Point point) {
    map.put(new SphericalPoint(session, point));
  }

  public static Entry findBySessionId(SessionId sessionId) {
    return map.findBySessionId(sessionId);
  }

  @SuppressWarnings("unchecked")
  public static <E extends Entry> List<E> findAround(SessionId sessionId, byte radius) {
    return (List<E>) map.findAround(sessionId, radius);
  }

  public static int size() {
    return map.size();
  }

  public static void clear() {
    map.clearAll();
  }

  static {
    Sessions.addListener(new SessionListener() {
      @Override
      public void onRemove(Session session) {
        map.remove(session.sessionId);
      }
    });
  }
}
