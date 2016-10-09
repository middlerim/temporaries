package com.middlerim.server.storage.location;

import java.util.List;

import com.middlerim.location.Point;
import com.middlerim.session.Session;
import com.middlerim.session.SessionId;

public interface LocationStorage<E extends LocationStorage.Entry> {

  public interface Entry {
    Session session();
    Point point();
  }

  E findBySessionId(SessionId sessionId);
  List<E> findAround(SessionId sessionId, byte radius);
  void put(E entry);
  boolean remove(SessionId sessionId);
  int size();
  void clearAll();
}
