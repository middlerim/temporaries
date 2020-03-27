package com.middlerim.server.command.storage;

import java.util.List;

import com.middlerim.location.Point;
import com.middlerim.server.command.Config;
import com.middlerim.server.command.message.Location;
import com.middlerim.server.command.storage.location.LocationStorage;
import com.middlerim.server.command.storage.location.SphericalPoint;
import com.middlerim.server.command.storage.location.VpTree;
import com.middlerim.server.storage.SessionListener;
import com.middlerim.server.storage.Sessions;
import com.middlerim.session.Session;
import com.middlerim.session.SessionId;
import com.middlerim.storage.persistent.FixedLayoutPersistentStorage;
import com.middlerim.storage.persistent.FixedLayoutStorageInformation;

public final class Locations {
  private static final LocationStorage<SphericalPoint> map = new VpTree<>();
  private static final FixedLayoutPersistentStorage<Location> persistentStorage = new FixedLayoutPersistentStorage<>(
      new FixedLayoutStorageInformation<Location>("locations",
          Location.SERIALIZED_BYTE_SIZE,
          Location.SERIALIZED_BYTE_SIZE * (Config.TEST ? 10_000 : SessionId.MAX_USER_SIZE), (Config.TEST ? 100_000 : 100_000_000)));

  public static void updateLocation(Session session, Location location) {
    map.put(new SphericalPoint(session, location.point));
    persistentStorage.put(location);
  }

  public static Point findBySessionId(SessionId sessionId) {
    LocationStorage.Entry onMemory = map.findBySessionId(sessionId);
    if (onMemory != null) {
      return onMemory.point();
    }
    Session session = Sessions.getSession(sessionId.userId());
    if (session == null) {
      return null;
    }
    Location location = persistentStorage.get(sessionId.userId(), Location.createEmpty(session));
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

      @Override
      public void onExpire(Session oldSession, Session newSession) {
        SphericalPoint onMemory = map.findBySessionId(oldSession.sessionId);
        if (onMemory != null) {
          map.remove(oldSession.sessionId);
          updateLocation(newSession, new Location(newSession, onMemory.point()));
        }
        persistentStorage.delete(oldSession.sessionId.userId());
      }
    });
  }
}
