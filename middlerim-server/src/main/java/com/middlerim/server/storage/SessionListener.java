package com.middlerim.server.storage;

import com.middlerim.session.Session;

public interface SessionListener {

  void onExpire(Session oldSession, Session newSession);
  void onRemove(Session session);
}
