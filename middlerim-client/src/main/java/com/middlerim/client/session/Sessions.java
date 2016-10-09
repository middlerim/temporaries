package com.middlerim.client.session;

import java.util.Random;

import com.middlerim.client.CentralServer;
import com.middlerim.session.Session;
import com.middlerim.session.SessionId;
import com.middlerim.util.Bytes;

public class Sessions {
    private volatile static Session singleton;

    public static Session getSession() {
        synchronized (Sessions.class) {
            if (singleton == null) {
                singleton = Session.create(getSessionId(), CentralServer.serverAddress);
            }
        }
        return singleton;
    }

    private static Random random = new Random();
    private static SessionId getSessionId() {
        long m = random.nextLong();
        byte[] sessionIdBytes = Bytes.longToBytes(m);
        sessionIdBytes[0] = 0;
        sessionIdBytes[1] = 0;
        SessionId sessionId = new SessionId(sessionIdBytes); // FIXME
        return sessionId;
    }
}
