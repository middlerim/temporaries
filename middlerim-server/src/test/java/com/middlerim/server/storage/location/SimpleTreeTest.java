package com.middlerim.server.storage.location;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import java.net.InetSocketAddress;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.middlerim.location.Point;
import com.middlerim.server.MessageCommands;
import com.middlerim.server.storage.location.SimpleTree.Entry;
import com.middlerim.session.Session;
import com.middlerim.session.SessionId;
import com.middlerim.util.Bytes;

import io.netty.channel.unix.DatagramSocketAddress;

public class SimpleTreeTest {

  private SimpleTree tree = new SimpleTree();

  @Before
  public void before() {
    tree.clearAll();
  }

  public static Session createDummySession(int i) {
    byte[] bs = Bytes.longToBytes(i);
    SessionId sessionId = new SessionId(Bytes.reverse(bs));
    return Session.create(sessionId, null);
  }

  @Test
  public void test_findBySessionId() {
    InetSocketAddress sender = DatagramSocketAddress.createUnresolved("localhost", 44445);
    SessionId userA = new SessionId(new byte[]{1, 2, 3, 4, 5, 6, 7, 8});
    SessionId userB = new SessionId(new byte[]{2, 2, 3, 4, 5, 6, 7, 8});

    {
      // 0
      Entry result = tree.findBySessionId(userA);
      assertNull(result);
    }
    {
      // 1
      Point location = new Point(1, 1);
      Session session = Session.create(userA, sender);
      tree.put(new SimpleTree.Entry(session, location));
      Entry result = tree.findBySessionId(userA);
      assertThat(result.point(), is(location));
      assertThat(result.session(), is(session));
      assertThat(tree.size(), is(1));
    }
    {
      // 2
      Point location = new Point(2, 2);
      Session session = Session.create(userB, sender);
      tree.put(new SimpleTree.Entry(session, location));
      Entry result = tree.findBySessionId(userB);
      assertThat(result.point(), is(location));
      assertThat(result.session(), is(session));
      assertThat(tree.size(), is(2));
    }
    {
      // Same user as 1
      Point location = new Point(3, 3);
      Session session = Session.create(userA, sender);
      tree.put(new SimpleTree.Entry(session, location));
      Entry result = tree.findBySessionId(userA);
      assertThat(result.point(), is(location));
      assertThat(result.session(), is(session));
      assertThat(tree.size(), is(2));
    }
  }

  @Test
  public void test_findAround() {
    InetSocketAddress sender = DatagramSocketAddress.createUnresolved("localhost", 44445);
    SessionId userA = new SessionId(new byte[]{1, 2, 3, 4, 5, 6, 7, 8});
    Point locationA = new Point(1, 1); // -> 2
    SessionId userB = new SessionId(new byte[]{2, 2, 3, 4, 5, 6, 7, 8});
    Point locationB = Point.forTest(1.3, 1); // -> 2.4
    SessionId userC = new SessionId(new byte[]{3, 2, 3, 4, 5, 6, 7, 8});
    Point locationC = Point.forTest(1.7, 1);
    SessionId userD = new SessionId(new byte[]{4, 2, 3, 4, 5, 6, 7, 8});
    Point locationD = Point.forTest(4, 1);
    SessionId userE = new SessionId(new byte[]{5, 2, 3, 4, 5, 6, 7, 8});
    Point locationE = Point.forTest(3.1, 1);

    int km = 80;
    {
      // 0
      List<Entry> result = tree.findAround(userA, MessageCommands.areaKM(km));
      assertThat(result.size(), is(0));
    }
    {
      // 1
      Session session = Session.create(userA, sender);
      tree.put(new SimpleTree.Entry(session, locationA));
      assertThat(tree.size(), is(1));
      List<Entry> result = tree.findAround(userA, MessageCommands.areaKM(km));
      assertThat(result.size(), is(0));
    }
    {
      // Set userB within 80km of userA.

      Session session = Session.create(userB, sender);
      tree.put(new SimpleTree.Entry(session, locationB));
      assertThat(tree.size(), is(2));
      List<Entry> resultA = tree.findAround(userA, MessageCommands.areaKM(km));
      assertThat(resultA.size(), is(1));
      assertThat(resultA.get(0).point(), is(locationB));

      List<Entry> resultB = tree.findAround(userB, MessageCommands.areaKM(km));
      assertThat(resultB.size(), is(1));
      assertThat(resultB.get(0).point(), is(locationA));
    }
    {
      // Set userB outside of userA.
      locationB = Point.forTest(2.4, 1);
      Session session = Session.create(userB, sender);
      tree.put(new SimpleTree.Entry(session, locationB));
      List<Entry> result = tree.findAround(userA, MessageCommands.areaKM(km));
      assertThat(result.size(), is(0));
    }
    {
      // Set userC within 80km of userA and userB.
      Session session = Session.create(userC, sender);
      tree.put(new SimpleTree.Entry(session, locationC));
      assertThat(tree.size(), is(3));
      List<Entry> resultA = tree.findAround(userA, MessageCommands.areaKM(km));
      assertThat(resultA.size(), is(1));
      assertThat(resultA.get(0).point(), is(locationC));
      List<Entry> resultB = tree.findAround(userB, MessageCommands.areaKM(km));
      assertThat(resultB.size(), is(1));
      assertThat(resultB.get(0).point(), is(locationC));
      List<Entry> resultC = tree.findAround(userC, MessageCommands.areaKM(km));
      assertThat(resultC.size(), is(2));
      assertThat(resultC.get(0).point(), is(locationA));
      assertThat(resultC.get(1).point(), is(locationB));
    }
    {
      // Move userA within 80km of userB and userC.
      // C < A < B
      Session session = Session.create(userA, sender);
      locationA = new Point(2, 1);
      tree.put(new SimpleTree.Entry(session, locationA));
      assertThat(tree.size(), is(3));
      List<Entry> resultA = tree.findAround(userA, MessageCommands.areaKM(km));
      assertThat(resultA.size(), is(2));
      assertThat(resultA.get(0).point(), is(locationC));
      assertThat(resultA.get(1).point(), is(locationB));
      List<Entry> resultB = tree.findAround(userB, MessageCommands.areaKM(km));
      assertThat(resultB.size(), is(2));
      assertThat(resultB.get(0).point(), is(locationC));
      assertThat(resultB.get(1).point(), is(locationA));
      List<Entry> resultC = tree.findAround(userC, MessageCommands.areaKM(km));
      assertThat(resultC.size(), is(2));
      assertThat(resultC.get(0).point(), is(locationA));
      assertThat(resultC.get(1).point(), is(locationB));
    }
    {
      // Set userD outside of others.
      // C < A < B < D
      Session session = Session.create(userD, sender);
      tree.put(new SimpleTree.Entry(session, locationD));
      assertThat(tree.size(), is(4));
      List<Entry> resultA = tree.findAround(userA, MessageCommands.areaKM(km));
      assertThat(resultA.size(), is(2));
      assertThat(resultA.get(0).point(), is(locationC));
      assertThat(resultA.get(1).point(), is(locationB));
      List<Entry> resultB = tree.findAround(userB, MessageCommands.areaKM(km));
      assertThat(resultB.size(), is(2));
      assertThat(resultB.get(0).point(), is(locationC));
      assertThat(resultB.get(1).point(), is(locationA));
      List<Entry> resultC = tree.findAround(userC, MessageCommands.areaKM(km));
      assertThat(resultC.size(), is(2));
      assertThat(resultC.get(0).point(), is(locationA));
      assertThat(resultC.get(1).point(), is(locationB));
      List<Entry> result = tree.findAround(userD, MessageCommands.areaKM(km));
      assertThat(result.size(), is(0));
    }
    {
      // Set userE between B and D.
      // C < A < B < E < D
      Session session = Session.create(userE, sender);
      tree.put(new SimpleTree.Entry(session, locationE));
      assertThat(tree.size(), is(5));
      List<Entry> resultA = tree.findAround(userA, MessageCommands.areaKM(km));
      assertThat(resultA.size(), is(2));
      assertThat(resultA.get(0).point(), is(locationC));
      assertThat(resultA.get(1).point(), is(locationB));
      List<Entry> resultB = tree.findAround(userB, MessageCommands.areaKM(km));
      assertThat(resultB.size(), is(3));
      assertThat(resultB.get(0).point(), is(locationC));
      assertThat(resultB.get(1).point(), is(locationA));
      assertThat(resultB.get(2).point(), is(locationE));
      List<Entry> resultC = tree.findAround(userC, MessageCommands.areaKM(km));
      assertThat(resultC.size(), is(2));
      assertThat(resultC.get(0).point(), is(locationA));
      assertThat(resultC.get(1).point(), is(locationB));
      List<Entry> result = tree.findAround(userD, MessageCommands.areaKM(km));
      assertThat(result.size(), is(0));
    }
  }
}
