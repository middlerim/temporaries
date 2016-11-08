package com.middlerim.server.command.storage.location;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import com.middlerim.location.Point;
import com.middlerim.server.MessageCommands;
import com.middlerim.session.Session;
import com.middlerim.session.SessionId;

public class SimpleTree implements LocationStorage<SimpleTree.Entry> {

  public static class Entry implements Comparable<Entry>, LocationStorage.Entry {
    private final Session session;
    private final Point point;
    public Entry(Session session, Point point) {
      this.session = session;
      this.point = point;
    }
    public Entry(Session session, int latitude, int longtitude) {
      this(session, new Point(latitude, longtitude));
    }

    @Override
    public Session session() {
      return session;
    }
    @Override
    public Point point() {
      return point;
    }
    @Override
    public int compareTo(Entry o) {
      int d = point.latitude - o.point.latitude;
      return d == 0 ? compareUserId(o) : d;
    }
    private int compareUserId(Entry o) {
      long d = session.sessionId.userId() - o.session.sessionId.userId();
      return d == 0 ? 0 : (d < 0 ? -1 : 1);
    }
    @Override
    public boolean equals(Object obj) {
      Entry b = (Entry) obj;
      return session.sessionId.userId() == b.session.sessionId.userId();
    }
    @Override
    public int hashCode() {
      return session.hashCode();
    }
    @Override
    public String toString() {
      return session.toString() + " " + point.toString();
    }
  }

  private static final Session DUMMY_SESSION = Session.create(new SessionId(new byte[]{0, 0, 0, 0, 0, 0, 0, 0}), null);
  private static final Integer DUMMY_VALUE = 0;
  private static final int MAX_METER_PAR_LATITUDE = (int)(((2 * Math.PI * 6378137d) / 360) * Point.GETA);

  private Map<SessionId, Entry> userLocationMap = new HashMap<>();
  private TreeMap<Entry, Integer> locs = new TreeMap<>();

  @Override
  public void put(Entry entry) {
    synchronized (locs) {
      Entry old = userLocationMap.put(entry.session.sessionId, entry);
      if (old != null) {
        locs.remove(old);
      }
      locs.put(entry, DUMMY_VALUE);
    }
  }

  @Override
  public boolean remove(SessionId sessionId) {
    synchronized (locs) {
      Entry entry = userLocationMap.remove(sessionId);
      if (entry != null) {
        locs.remove(entry);
        return true;
      }
      return false;
    }
  }

  @Override
  public Entry findBySessionId(SessionId sessionId) {
    return userLocationMap.get(sessionId);
  }

  @Override
  public List<SimpleTree.Entry> findAround(SessionId sessionId, byte radius) {
    List<SimpleTree.Entry> result = new ArrayList<>();
    Entry user = findBySessionId(sessionId);
    if (user == null) {
      return Collections.emptyList();
    }

    int meter = MessageCommands.toMeter(radius);
    int deltaLatitude = meter / MAX_METER_PAR_LATITUDE;
    Entry minLatitude = new Entry(DUMMY_SESSION, new Point(user.point.latitude - deltaLatitude, user.point.longitude));
    Entry maxLatitude = new Entry(DUMMY_SESSION, new Point(user.point.latitude + deltaLatitude, user.point.longitude));
    SortedMap<Entry, Integer> subMap = locs.subMap(minLatitude, false, maxLatitude, false);
    for (Entry entry : subMap.keySet()) {
      if (user.session.sessionId.equals(entry.session.sessionId)) {
        continue;
      }
      if (user.point.distanceMeter(entry.point) <= meter) {
        result.add(entry);
      } else {
        break;
      }
    }
    return result;
  }

  @Override
  public int size() {
    return locs.size();
  }

  @Override
  public void clearAll() {
    locs.clear();
    userLocationMap.clear();
  }
}
