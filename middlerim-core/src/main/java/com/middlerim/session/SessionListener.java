package com.middlerim.session;

public interface SessionListener {

  void onExpire(Session oldSession, Session newSession);
  void onRemove(Session session);
}
