package com.middlerim.integration;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import org.junit.BeforeClass;
import org.junit.Test;

import com.middlerim.client.Config;
import com.middlerim.client.session.Sessions;
import com.middlerim.session.Session;

public class SessionExpiredTest extends End2EndTest {

  @BeforeClass
  public static void beforeClass() throws Exception {
    System.setProperty(Config.KEY_SESSION_TIMEOUT_MILLIS, "" + 10 * 1000);
    System.setProperty(Config.KEY_KEEP_ALIVE_ENABLED, "false");
    run();
  }

  @Test
  public void testSessionTimeout() throws InterruptedException {
    updateLocation(MARUNOUCHI_2_4_1_TOKYO);
    Session session1 = Sessions.getSession();
    Thread.sleep(Config.SESSION_TIMEOUT_MILLIS * 2);
    updateLocation(MARUNOUCHI_2_4_2_TOKYO);
    Session session2 = Sessions.getSession();
    assertThat(session2.sessionId.userId(), is(session1.sessionId.userId() + 1));
  }
}
