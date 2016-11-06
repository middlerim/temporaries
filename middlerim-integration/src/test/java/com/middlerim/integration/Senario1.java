package com.middlerim.integration;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import org.junit.BeforeClass;
import org.junit.Test;

import com.middlerim.client.session.Sessions;
import com.middlerim.client.view.ViewEvents;
import com.middlerim.location.Point;
import com.middlerim.server.MessageCommands;
import com.middlerim.server.storage.Locations;
import com.middlerim.session.Session;
import com.middlerim.session.SessionId;

public class Senario1 extends End2EndTest {

  @BeforeClass
  public static void beforeClass() throws Exception {
    run();
  }

  @Test
  public void testSendMessage() throws Exception {
    updateLocation(MARUNOUCHI_2_4_1_TOKYO);

    Session session = Sessions.getSession();
    Point point = Locations.findBySessionId(session.sessionId);
    assertThat(point, is(MARUNOUCHI_2_4_1_TOKYO.toPoint()));

    SessionId userB = createDummyUser();
    setLocation(userB, MARUNOUCHI_2_4_1_TOKYO);

    // Send a message to userB
    {
      sendMessage(MessageCommands.areaM(32));
      assertThat(ctx.lastReceivedTextEvent.numberOfDelivery, is(1));
    }

    SessionId userC = createDummyUser();
    setLocation(userC, MARUNOUCHI_2_4_2_TOKYO);
    // Area 32m: Don't send a message to userC
    {
      sendMessage(MessageCommands.areaM(32));
      assertThat(ctx.lastReceivedTextEvent.numberOfDelivery, is(1));
    }
    // Area 500m: Send a message to userC
    {
      sendMessage(MessageCommands.areaM(500));
      assertThat(ctx.lastReceivedTextEvent.numberOfDelivery, is(2));
    }
  }

  @Test
  public void testInvalidSequenceNo() throws InterruptedException {
    updateLocation(MARUNOUCHI_2_4_1_TOKYO);

    SessionId userB = createDummyUser();
    setLocation(userB, MARUNOUCHI_2_4_1_TOKYO);

    ctx.lastReceivedTextEvent = null;
    sendMessage(MessageCommands.areaM(32));
    assertTrue(ctx.lastReceivedTextEvent.numberOfDelivery > 0);

    // Force increment the sequenceNo.
    Session session = Sessions.getSession();
    short original = session.sessionId.clientSequenceNo();
    session.sessionId.synchronizeClientWithServer((byte) (session.sessionId.clientSequenceNo() - 99));
    ctx.lastReceivedTextEvent = null;
    sendMessage(MessageCommands.areaM(32));
    // Client sequenceNo is synchronized with the server.
    assertThat(session.sessionId.clientSequenceNo(), is((byte) (original + 1)));
    assertTrue(ctx.lastReceivedTextEvent.numberOfDelivery > 0);

    // Try again.
    short valid = session.sessionId.clientSequenceNo();
    ctx.lastReceivedTextEvent = null;
    sendMessage(MessageCommands.areaM(32));
    assertThat(session.sessionId.clientSequenceNo(), is((byte) (valid + 1)));
    assertTrue(ctx.lastReceivedTextEvent.numberOfDelivery > 0);
  }

  @Test
  public void testAsyncSendingAndReceiving() throws InterruptedException {
    updateLocation(MARUNOUCHI_2_4_1_TOKYO);

    SessionId userB = createDummyUser();
    setLocation(userB, MARUNOUCHI_2_4_1_TOKYO);
    for (int i = 0; i < 10; i++) {
      assertThat(sendMessage(userB, MessageCommands.areaM(32)), is(1));
      sendMessageAsync(MessageCommands.areaM(32));
    }
    Thread.sleep(10);
    Session serverSessionUserA = com.middlerim.server.storage.Sessions.getSession(Sessions.getSession().sessionId.userId());
    assertThat(Sessions.getSession().sessionId.clientSequenceNo(), is(serverSessionUserA.sessionId.clientSequenceNo()));

    Session serverSessionUserB = com.middlerim.server.storage.Sessions.getSession(userB.userId());
    assertThat(userB.clientSequenceNo(), is(serverSessionUserB.sessionId.clientSequenceNo()));

    setLocation(userB, MARUNOUCHI_2_4_2_TOKYO);
    for (int i = 0; i < 10; i++) {
      assertThat(sendMessage(userB, MessageCommands.areaM(32)), is(0));
      sendMessageAsync(MessageCommands.areaM(32));
    }
    Thread.sleep(10);
    serverSessionUserA = com.middlerim.server.storage.Sessions.getSession(Sessions.getSession().sessionId.userId());
    assertThat(Sessions.getSession().sessionId.clientSequenceNo(), is(serverSessionUserA.sessionId.clientSequenceNo()));

    serverSessionUserB = com.middlerim.server.storage.Sessions.getSession(userB.userId());
    assertThat(userB.clientSequenceNo(), is(serverSessionUserB.sessionId.clientSequenceNo()));
  }

  @Test
  public void testExit() throws InterruptedException {
    updateLocation(MARUNOUCHI_2_4_1_TOKYO);

    Session session = Sessions.getSession();
    Point point = Locations.findBySessionId(session.sessionId);
    assertThat(point, is(MARUNOUCHI_2_4_1_TOKYO.toPoint()));

    ViewEvents.fireDestroy();
    Thread.sleep(10);
    point = Locations.findBySessionId(session.sessionId);
    assertNull(point);
  }
}
