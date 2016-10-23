package com.middlerim.server.storage;

import java.util.List;

import com.middlerim.location.Point;
import com.middlerim.server.Config;
import com.middlerim.server.message.Location;
import com.middlerim.server.storage.location.LocationStorage;
import com.middlerim.server.storage.location.SphericalPoint;
import com.middlerim.server.storage.location.VpTree;
import com.middlerim.server.storage.persistent.FixedLayoutPersistentStorage;
import com.middlerim.server.storage.persistent.StorageInformation;
import com.middlerim.session.Session;
import com.middlerim.session.SessionId;
import com.middlerim.session.SessionListener;

public final class Locations {
  private static final LocationStorage<SphericalPoint> map = new VpTree<>();
  private static final FixedLayoutPersistentStorage<Location> persistentStorage = new FixedLayoutPersistentStorage<>(
      new StorageInformation<Location>("locations",
          Location.SERIALIZED_BYTE_SIZE,
          Location.SERIALIZED_BYTE_SIZE * (Config.TEST ? 10000 : SessionId.MAX_USER_SIZE)));

  public static void updateLocation(Session session, Location location) {
    map.put(new SphericalPoint(session, location.point));
    persistentStorage.put(location);
  }

  public static Point findBySessionId(SessionId sessionId) {
    LocationStorage.Entry onMemory = map.findBySessionId(sessionId);
    if (onMemory != null) {
      return onMemory.point();
    }
    Session session = Sessions.getSession(sessionId);
    if (session == null) {
      return null;
    }
    Location location = persistentStorage.get(sessionId.userId(), Location.createEmpty(sessionId));
    updateLocation(session, location);
    return location.point;
  }

  @SuppressWarnings("unchecked")
  public static <E extends LocationStorage.Entry> List<E> findAround(SessionId sessionId, byte radius) {
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
        persistentStorage.delete(session.sessionId.userId());
      }
    });
  }
}
