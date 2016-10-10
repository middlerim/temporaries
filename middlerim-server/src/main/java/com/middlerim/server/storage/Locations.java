package com.middlerim.server.storage;

import java.util.List;

import com.middlerim.server.Config;
import com.middlerim.server.message.Location;
import com.middlerim.server.storage.location.LocationStorage;
import com.middlerim.server.storage.location.LocationStorage.Entry;
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
      new StorageInformation<>("locations",
          Location.SERIALIZED_BYTE_SIZE,
          Location.SERIALIZED_BYTE_SIZE * (Config.TEST ? 100 : SessionId.MAX_USER_SIZE)));

  public static void updateLocation(Session session, Location location) {
    map.put(new SphericalPoint(session, location.point));
    persistentStorage.put(location);
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
        persistentStorage.delete(session.sessionId.userId());
      }
    });
  }
}
