package com.middlerim.client.session;

import com.middlerim.client.Config;
import com.middlerim.session.Session;
import com.middlerim.session.SessionId;

public class Sessions {
  private volatile static Session singleton;

  public static Session setAnonymous() {
    singleton = Session.create(SessionId.ANONYMOUS, Config.COMMAND_SERVER);
    return singleton;
  }

  public static void setSession(Session session) {
    singleton = session;
  }

  public static Session getSession() {
    return singleton;
  }
}
