package com.middlerim.server.storage;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.middlerim.server.Config;
import com.middlerim.session.Session;
import com.middlerim.session.SessionId;
import com.middlerim.session.SessionListener;
import com.middlerim.storage.persistent.IdStorage;
import com.middlerim.storage.persistent.IdStorageInformation;

public final class Sessions {
  private static LinkedList<Session> sessionQueue = new LinkedList<>();
  private static final IdStorage aidStorage = new IdStorage(
      Config.TEST
          ? new IdStorageInformation("aid", 1, 10000)
          : new IdStorageInformation("aid", SessionId.ANONYMOUS_USER_FROM, SessionId.ANONYMOUS_USER_TO));

  private static Map<SessionId, Session> sessionMap = new HashMap<>();

  private static List<SessionListener> listeners = new ArrayList<>();

  public static Session getSession(SessionId sessionId) {
    return sessionMap.get(sessionId);
  }

  private static SessionId createAnonymousSessionId() throws IOException {
    long id = aidStorage.incrementAndGet();
    return new SessionId((int) id);
  }

  public static Session getOrCreateAnonymous(InetSocketAddress address) throws IOException {
    Session anonymous = getOrCreateSession0(createAnonymousSessionId(), address);
    return anonymous;
  }

  public static Session getOrCreateSession(SessionId sessionId, InetSocketAddress address) throws IOException {
    if (sessionId == SessionId.ANONYMOUS) {
      throw new IllegalArgumentException();
    }
    return getOrCreateSession0(sessionId, address);
  }

  private static Session getOrCreateSession0(SessionId sessionId, InetSocketAddress address) throws IOException {
    synchronized (sessionMap) {
      Session session = sessionMap.get(sessionId);
      if (session == null) {
        session = Session.create(sessionId, address);
        sessionMap.put(sessionId, session);
        sessionQueue.add(session);
        if (sessionQueue.size() >= Config.MAX_ACTIVE_SESSION_SIZE) {
          Session last = sessionQueue.poll();
          remove(last);
        }
        long currentTimeMillis = System.currentTimeMillis();
        while (true) {
          Session last = sessionQueue.getLast();
          if (last.createTimeMillis + Config.SESSION_TIMEOUT_MILLIS < currentTimeMillis) {
            sessionQueue.poll();
            remove(last);
          } else {
            break;
          }
        }
      } else {
        session.address = address;
      }
      return session;
    }
  }

  public static void addListener(SessionListener l) {
    listeners.add(l);
  }

  public static void remove(Session session) {
    for (SessionListener l : listeners) {
      l.onRemove(session);
    }
    synchronized (sessionMap) {
      sessionMap.remove(session.sessionId);
    }
  }

  public static void clear() {
    sessionQueue.clear();
    sessionMap.clear();
  }
}
