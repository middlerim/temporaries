package com.middlerim.client.session;

import com.middlerim.session.Session;

public class Sessions {
    private volatile static Session singleton;

    public static void setSession(Session session) {
      singleton = session;
    }

    public static Session getSession() {
        return singleton;
    }
}
