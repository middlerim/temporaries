package com.middlerim.session;

import java.net.InetSocketAddress;

public class Session {
  public final SessionId sessionId;
  public final long createdTimeMillis;
  public long lastAccessTimeMillis;
  public InetSocketAddress address;

  public static Session create(SessionId sessionId, InetSocketAddress address) {
    sessionId.status = SessionId.NEW;
    return new Session(sessionId, System.currentTimeMillis(), address);
  }

  private Session(SessionId sessionId, long createdTimeMillis, InetSocketAddress address) {
    this.sessionId = sessionId;
    this.createdTimeMillis = createdTimeMillis;
    this.lastAccessTimeMillis = createdTimeMillis;
    this.address = address;
  }

  public void touch() {
    sessionId.status = SessionId.DEFAULT;
    this.lastAccessTimeMillis = System.currentTimeMillis();
  }

  public boolean isNotAssigned() {
    return sessionId.userId() == SessionId.UNASSIGNED_USERID;
  }

  public boolean isNew() {
    return sessionId.status == SessionId.NEW;
  }

  @Override
  public int hashCode() {
    return sessionId.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    Session b = (Session) obj;
    return sessionId.equals(b.sessionId);
  }
  @Override
  public String toString() {
    return sessionId + "#" + address;
  }
}
