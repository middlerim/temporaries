package com.middlerim.integration;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.middlerim.client.Config;
import com.middlerim.client.central.CentralEvents;
import com.middlerim.client.central.CentralServer;
import com.middlerim.client.message.Location;
import com.middlerim.client.session.Sessions;
import com.middlerim.client.view.ViewEvents;
import com.middlerim.location.Coordinate;
import com.middlerim.location.Point;
import com.middlerim.server.Headers;
import com.middlerim.server.command.CommandServer;
import com.middlerim.server.command.storage.Locations;
import com.middlerim.session.SessionId;
import com.middlerim.token.TokenIssuer;
import com.middlerim.util.Bytes;

import io.netty.buffer.Unpooled;

@FixMethodOrder(MethodSorters.JVM)
public abstract class End2EndTest {
  private static final Logger LOG = LoggerFactory.getLogger(End2EndTest.class);
  protected static IntegrationContext ctx;
  private static CommandServer commandServer;

  public static void run() throws Exception {
    ctx = new IntegrationContext();

    CountDownLatch latch = new CountDownLatch(2);
    new Thread(new Runnable() {

      @Override
      public void run() {
        commandServer = new CommandServer();
        commandServer.runEmbedded();
        latch.countDown();
      }
    }, "CentralServer").run();

    CentralEvents.onStarted("End2EndTest.CentralEvents.Listener<CentralEvents.StartedEvent>", new CentralEvents.Listener<CentralEvents.StartedEvent>() {
      @Override
      public void handle(CentralEvents.StartedEvent event) {
        latch.countDown();
      }
    });
    CentralServer.run(ctx);

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
    commandServer.shutdown();
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
  protected static final Coordinate EMPTY = new Coordinate(0, 0);
  protected static final Coordinate MARUNOUCHI_2_4_1_TOKYO = new Coordinate(35.681235, 139.763995);
  protected static final Coordinate MARUNOUCHI_2_4_2_TOKYO = new Coordinate(35.67733, 139.765214);

  protected SessionId createDummyUser() {
    DatagramSocket clientSocket = null;
    try {
      clientSocket = new DatagramSocket();
      DatagramPacket assingAidPacket = new DatagramPacket(new byte[]{Headers.Control.ASSIGN_AID.code}, 1, Config.COMMAND_SERVER);
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

  protected void setLocation(SessionId sessionId, Coordinate location) {
    setLocation(sessionId, location, Config.COMMAND_SERVER);
  }
  protected void setLocation(SessionId sessionId, Coordinate location, InetSocketAddress address) {
    DatagramSocket clientSocket = null;
    try {
      clientSocket = new DatagramSocket();
      byte[] sessionIdBytes = new byte[8];
      sessionId.readBytes(sessionIdBytes);
      Point point = location.toPoint();
      byte[] data = Unpooled.buffer(Location.FIXED_BYTE_SIZE).writeByte(Headers.Command.LOCATION.code)
          .writeBytes(sessionIdBytes)
          .writeInt(point.latitude)
          .writeInt(point.longitude).array();
      DatagramPacket assingAidPacket = new DatagramPacket(data, data.length, address);
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

  private int tag = Integer.MAX_VALUE;
  protected int sendMessage(SessionId sessionId, byte messageCommand) {
    return sendMessage(sessionId, messageCommand, Config.COMMAND_SERVER);
  }
  protected int sendMessage(SessionId sessionId, byte messageCommand, InetSocketAddress address) {
    DatagramSocket clientSocket = null;
    try {
      clientSocket = new DatagramSocket();
      byte[] sessionIdBytes = new byte[8];
      sessionId.incrementClientSequenceNo();
      sessionId.readBytes(sessionIdBytes);

      byte[] data = Unpooled.buffer(15)
          .writeByte(Headers.Command.TEXT.code)
          .writeBytes(sessionIdBytes)
          .writeInt(tag++)
          .writeByte(messageCommand)
          .writeByte((byte) 2)
          .writeChar('あ')
          .writeChar('い').array();
      DatagramPacket assingAidPacket = new DatagramPacket(data, data.length, address);
      clientSocket.send(assingAidPacket);
      byte[] receivedData = new byte[9];
      DatagramPacket receivePacket = new DatagramPacket(receivedData, receivedData.length);
      clientSocket.receive(receivePacket);
      int numberOfReceiver = Bytes.bytesToInt(new byte[]{receivedData[5], receivedData[6], receivedData[7], receivedData[8]});
      LOG.debug("Sent dummy message and delivered to {} users", numberOfReceiver);
      return numberOfReceiver;
    } catch (Exception e) {
      throw new RuntimeException(e);
    } finally {
      if (clientSocket != null) {
        clientSocket.close();
      }
    }
  }

  protected void updateLocation(Coordinate location) throws InterruptedException {
    ViewEvents.fireLocationUpdate(location);
    Thread.sleep(500);
  }

  protected void sendMessage(byte messageCommand) throws InterruptedException {
    sendMessage(messageCommand, true);
  }
  protected void sendMessageAsync(byte messageCommand) throws InterruptedException {
    sendMessage(messageCommand, false);
  }
  protected void sendMessage(byte messageCommand, boolean wait) throws InterruptedException {
    String displayName = "あいう";
    ByteBuffer messageBuf = ByteBuffer.allocate(3).putChar('✌');
    byte[] message = new byte[messageBuf.position()];
    messageBuf.position(0);
    messageBuf.get(message);
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
}
