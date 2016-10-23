package com.middlerim.session;

import java.net.InetSocketAddress;

public class Session {
  public final SessionId sessionId;
  public final long createTimeMillis;
  public InetSocketAddress address;

  public static Session create(SessionId sessionId, InetSocketAddress address) {
    sessionId.status = SessionId.NEW;
    return new Session(sessionId, System.currentTimeMillis(), address);
  }

  private Session(SessionId sessionId, long createTimeMillis, InetSocketAddress address) {
    this.sessionId = sessionId;
    this.createTimeMillis = createTimeMillis;
    this.address = address;
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
