package com.middlerim.integration;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.middlerim.client.CentralEvents;
import com.middlerim.client.CentralServer;
import com.middlerim.client.message.Location;
import com.middlerim.client.session.Sessions;
import com.middlerim.client.view.ViewEvents;
import com.middlerim.location.Coordinate;
import com.middlerim.location.Point;
import com.middlerim.server.Headers;
import com.middlerim.server.MessageCommands;
import com.middlerim.server.UdpServer;
import com.middlerim.server.storage.Locations;
import com.middlerim.session.Session;
import com.middlerim.session.SessionId;
import com.middlerim.token.TokenIssuer;
import com.middlerim.util.Bytes;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;

@FixMethodOrder(MethodSorters.JVM)
public class End2EndTest {
  private static final Logger LOG = LoggerFactory.getLogger(End2EndTest.class);
  private static IntegrationContext ctx;

  @BeforeClass
  public static void beforeClass() throws Exception {
    ctx = new IntegrationContext();

    CountDownLatch latch = new CountDownLatch(2);
    new Thread(new Runnable() {

      @Override
      public void run() {
        UdpServer.runEmbedded();
        latch.countDown();
      }
    }, "CentralServer").run();
    new Thread(new Runnable() {

      @Override
      public void run() {
        CentralServer.run(ctx).addListener(new ChannelFutureListener() {
          @Override
          public void operationComplete(ChannelFuture future) throws Exception {
            latch.countDown();
          }
        });
      }
    }, "Client").run();
    latch.await();

    CentralEvents.onReceiveMessage("End2EndTest.CentralEvents.Listener<CentralEvents.ReceiveMessageEvent>", new CentralEvents.Listener<CentralEvents.ReceiveMessageEvent>() {

      @Override
      public void handle(CentralEvents.ReceiveMessageEvent event) {
        ctx.lastReceiveMessageEvent = event;
      }
    });

    CentralEvents.onStarted("End2EndTest.CentralEvents.Listener<CentralEvents.StartedEvent>", new CentralEvents.Listener<CentralEvents.StartedEvent>() {
      @Override
      public void handle(CentralEvents.StartedEvent event) {
        ViewEvents.fireResume();
      }
    });
  }

  @AfterClass
  public static void afterClass() {
    if (CentralServer.isStarted()) {
      ViewEvents.fireDestroy();
    }
    UdpServer.shutdown();
  }

  @Before
  public void before() throws InterruptedException {
    Locations.clear();
    Sessions.setAnonymous();
    ViewEvents.fireLocationUpdate(EMPTY);
    while (true) {
      if (!Sessions.getSession().isNotAssigned()) {
        break;
      }
      Thread.sleep(100);
    }
    LOG.debug("Clear all locations. Set new anonymous session. Ready for test.");
  }
  private static final Coordinate EMPTY = new Coordinate(0, 0);
  private static final Coordinate MARUNOUCHI_2_4_1_TOKYO = new Coordinate(35.681235, 139.763995);
  private static final Coordinate MARUNOUCHI_2_4_2_TOKYO = new Coordinate(35.67733, 139.765214);

  private SessionId createDummyUser() {
    DatagramSocket clientSocket = null;
    try {
      clientSocket = new DatagramSocket();
      DatagramPacket assingAidPacket = new DatagramPacket(new byte[]{Headers.ASSIGN_AID}, 1, CentralServer.serverIPv4Address);
      clientSocket.send(assingAidPacket);
      byte[] receivedData = new byte[9];
      DatagramPacket receivePacket = new DatagramPacket(receivedData, receivedData.length);
      clientSocket.receive(receivePacket);
      SessionId sessionId = TokenIssuer.decodeTosessionId(Arrays.copyOfRange(receivedData, 1, receivedData.length));
      LOG.debug("Created new dummy user {}", sessionId);
      return sessionId;
    } catch (Exception e) {
      throw new RuntimeException(e);
    } finally {
      if (clientSocket != null) {
        clientSocket.close();
      }
    }
  }

  private void setLocation(SessionId sessionId, Coordinate location) {
    DatagramSocket clientSocket = null;
    try {
      clientSocket = new DatagramSocket();
      byte[] sessionIdBytes = new byte[8];
      sessionId.readBytes(sessionIdBytes);
      Point point = location.toPoint();
      byte[] data = Unpooled.buffer(Location.FIXED_BYTE_SIZE).writeByte(Headers.LOCATION)
          .writeBytes(sessionIdBytes)
          .writeInt(point.latitude)
          .writeInt(point.longitude).array();
      DatagramPacket assingAidPacket = new DatagramPacket(data, data.length, CentralServer.serverIPv4Address);
      clientSocket.send(assingAidPacket);
      Thread.sleep(50);
      LOG.debug("Set location {} for the dummy user {}", location, sessionId);
    } catch (Exception e) {
      throw new RuntimeException(e);
    } finally {
      if (clientSocket != null) {
        clientSocket.close();
      }
    }
  }

  private int sendMessage(SessionId sessionId, byte messageCommand) {
    DatagramSocket clientSocket = null;
    try {
      clientSocket = new DatagramSocket();
      byte[] sessionIdBytes = new byte[8];
      sessionId.readBytes(sessionIdBytes);
      byte[] data = Unpooled.buffer(15)
          .writeByte(Headers.mask(Headers.TEXT, Headers.COMPLETE))
          .writeBytes(sessionIdBytes)
          .writeByte(messageCommand)
          .writeByte((byte) 2)
          .writeChar('あ')
          .writeChar('い').array();
      DatagramPacket assingAidPacket = new DatagramPacket(data, data.length, CentralServer.serverIPv4Address);
      clientSocket.send(assingAidPacket);
      byte[] receivedData = new byte[6];
      DatagramPacket receivePacket = new DatagramPacket(receivedData, receivedData.length);
      clientSocket.receive(receivePacket);
      int numberOfReceiver = Bytes.bytesToInt(new byte[]{receivedData[2], receivedData[3], receivedData[4], receivedData[5]});
      LOG.debug("Sent dummy message and delivered to {} users", numberOfReceiver);
      sessionId.incrementClientSequenceNo();
      return numberOfReceiver;
    } catch (Exception e) {
      throw new RuntimeException(e);
    } finally {
      if (clientSocket != null) {
        clientSocket.close();
      }
    }
  }

  private void updateLocation(Coordinate location) throws InterruptedException {
    ViewEvents.fireLocationUpdate(location);
    Thread.sleep(500);
  }

  private void sendMessage(byte messageCommand) throws InterruptedException {
    sendMessage(messageCommand, true);
  }
  private void sendMessageAsync(byte messageCommand) throws InterruptedException {
    sendMessage(messageCommand, false);
  }
  private void sendMessage(byte messageCommand, boolean wait) throws InterruptedException {
    String displayName = "あいう";
    ByteBuffer message = ByteBuffer.allocate(3).putChar('✌');
    ViewEvents.fireSubmitMessage(0, displayName, messageCommand, message);
    ctx.lastReceivedTextEvent = null;
    if (wait) {
      CountDownLatch latch = new CountDownLatch(1);
      CentralEvents.Listener<CentralEvents.ReceivedTextEvent> l = new CentralEvents.Listener<CentralEvents.ReceivedTextEvent>() {
        @Override
        public void handle(CentralEvents.ReceivedTextEvent event) {
          ctx.lastReceivedTextEvent = event;
          latch.countDown();
        }
      };
      String tmpEventListener = "tmpReceivedTextEvent";
      CentralEvents.onReceivedText(tmpEventListener, l);
      try {
        boolean success = latch.await(1000, TimeUnit.SECONDS);
        if (!success) {
          throw new RuntimeException("Timeout: Message could't reach the central server.");
        }
      } finally {
        CentralEvents.removeListener(tmpEventListener);
      }
    } else {
      Thread.sleep(0);
    }
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
    Session serverSessionUserA = com.middlerim.server.storage.Sessions.getSession(Sessions.getSession().sessionId);
    assertThat(Sessions.getSession().sessionId.clientSequenceNo(), is(serverSessionUserA.sessionId.clientSequenceNo()));

    Session serverSessionUserB = com.middlerim.server.storage.Sessions.getSession(userB);
    assertThat(userB.clientSequenceNo(), is((byte) (serverSessionUserB.sessionId.clientSequenceNo() + 1)));

    setLocation(userB, MARUNOUCHI_2_4_2_TOKYO);
    for (int i = 0; i < 10; i++) {
      assertThat(sendMessage(userB, MessageCommands.areaM(32)), is(0));
      sendMessageAsync(MessageCommands.areaM(32));
    }
    Thread.sleep(10);
    serverSessionUserA = com.middlerim.server.storage.Sessions.getSession(Sessions.getSession().sessionId);
    assertThat(Sessions.getSession().sessionId.clientSequenceNo(), is(serverSessionUserA.sessionId.clientSequenceNo()));

    serverSessionUserB = com.middlerim.server.storage.Sessions.getSession(userB);
    assertThat(userB.clientSequenceNo(), is((byte) (serverSessionUserB.sessionId.clientSequenceNo() + 1)));
  }

  @Test
  public void testAgainRequestFromClient() throws InterruptedException {
    updateLocation(MARUNOUCHI_2_4_1_TOKYO);
    // TODO
    Session session = Sessions.getSession();
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
