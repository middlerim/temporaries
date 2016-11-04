package com.middlerim.server.storage;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.middlerim.server.Config;
import com.middlerim.session.Session;
import com.middlerim.session.SessionId;
import com.middlerim.session.SessionListener;
import com.middlerim.storage.persistent.IdStorage;
import com.middlerim.storage.persistent.IdStorageInformation;

/**
 * 
 * 3人までアクティブなユーザーを持てるとする。
 * 4人目が来たら1人目のセッションを削除する。
 * その後1人目が再度来たら2人目のセッションを削除する。
 * 1人目のセッションIDは再度採番(5)される。
 * セッションIDは1から5までの間で再利用しながら採番する。
 * 一定期間(30min程度)アクセスの無いセッションは削除する。
 * (!) この期間はセッションIDが1周する期間より短く設定する必要がある。
 * セッションが削除されるとメッセージのpublishができなくなるため、これを防ぐ(一定期間アクセスの無い状態を防ぐ)ためにユーザーの端末から定期的にリクエストを送信する。
 * 端末の状態により以下のケースではアクセスが来ないが、これらのケースでは仮にpublishされたとしても端末にメッセージが届くことがないため問題ない。
 * - インターネットに繋がっていない
 * - アプリが強制的に終了された
 * とは言ってもこの期間にpublishされるはずだったメッセージは端末が復活したタイミングで送信したいので、指定時間内のメッセージを能動的に取得できる仕組みは必要。
 */
public final class Sessions {
  private static final Logger LOG = LoggerFactory.getLogger(Sessions.class);
  private static Timer timer = new Timer("session-cleaner", true);
  private static LinkedList<Session> sessionQueue = new LinkedList<>();
  private static final IdStorage aidStorage = new IdStorage(
      Config.TEST
          ? new IdStorageInformation("aid", 1, 10000)
          : new IdStorageInformation("aid", SessionId.ANONYMOUS_USER_FROM, SessionId.ANONYMOUS_USER_TO));

  private static Map<SessionId, Session> sessionMap = new HashMap<>(Config.TEST ? 10000 : Config.MAX_ACTIVE_SESSION_SIZE);

  private static List<SessionListener> listeners = new ArrayList<>();

  static {
    timer.schedule(new TimerTask() {
      @Override
      public void run() {
        try {
          long currentTimeMillis = System.currentTimeMillis();
          while (true) {
            Session last = sessionQueue.peek();
            if (last == null) {
              return;
            }
            if (last.lastAccessTimeMillis - last.createdTimeMillis > Config.SESSIONID_RENEW_CYCLE_MILLIS) {
              LOG.debug("The session {} is using the session ID for a long time. It's time to assign a new session ID.", last);
              sessionQueue.poll();
              synchronized (sessionMap) {
                sessionMap.remove(last.sessionId);
              }
              expire(last, getOrCreateSession(last.sessionId, last.address));
            } else if (last.lastAccessTimeMillis + Config.SESSION_TIMEOUT_MILLIS <= currentTimeMillis) {
              LOG.info("Session timeout: {}", last);
              sessionQueue.poll();
              remove(last);
            } else {
              return;
            }
          }
        } catch (Exception e) {
          LOG.error("Unexpected exception", e);
        }
      }
    }, Config.SESSION_TIMEOUT_MILLIS, Config.SESSION_TIMEOUT_MILLIS / 2);
  }

  public static Session getSession(SessionId sessionId) {
    return sessionMap.get(sessionId);
  }

  private static SessionId createAnonymousSessionId() throws IOException {
    long id = aidStorage.incrementAndGet();
    return new SessionId((int) id);
  }

  public static Session getOrCreateAnonymous(InetSocketAddress address) throws IOException {
    Session anonymous = getOrCreateSession0(null, address);
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
      Session session = sessionId != null ? sessionMap.get(sessionId) : null;
      if (session == null) {
        if (sessionId == null) {
          sessionId = createAnonymousSessionId();
        } else if (sessionId.userId() != SessionId.UNASSIGNED_USERID) {
          // Session is new or removed. Don't accept reuse old session ID.Assign new session ID.
          sessionId = assignNewSessionId(sessionId);
        }
        session = Session.create(sessionId, address);
        sessionMap.put(sessionId, session);
        sessionQueue.add(session);
        if (sessionQueue.size() >= Config.MAX_ACTIVE_SESSION_SIZE) {
          Session last = sessionQueue.poll();
          LOG.info("Active session limit: {}", last);
          remove(last);
        }
      } else {
        session.address = address;
        session.touch();
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

  private static void expire(Session oldSession, Session newSession) {
    for (SessionListener l : listeners) {
      l.onExpire(oldSession, newSession);
    }
  }

  public static void clear() {
    sessionQueue.clear();
    sessionMap.clear();
  }

  private static SessionId assignNewSessionId(SessionId sessionId) throws IOException {
    SessionId tmp = createAnonymousSessionId();
    byte[] userIdBytes = new byte[4];
    tmp.readUserIdBytes(userIdBytes);
    return sessionId.copyWithNewUserId(userIdBytes);
  }
}
