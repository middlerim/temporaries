package com.middlerim.server.storage.location;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.middlerim.location.Point;
import com.middlerim.server.MessageCommands;
import com.middlerim.session.Session;
import com.middlerim.session.SessionId;
import com.middlerim.util.Bytes;

import io.netty.channel.unix.DatagramSocketAddress;

public class VpTreeTest {

  public static Session createDummySession(int i) {
    byte[] bs = Bytes.longToBytes(i);
    SessionId sessionId = new SessionId(Bytes.reverse(bs));
    return Session.create(sessionId, null);
  }

  @Test
  public void testBuildAndFind() {
    ArrayList<SphericalPoint> points = new ArrayList<SphericalPoint>();
    int size = 10000;
    int i = 0;
    SphericalPoint p1 = new SphericalPoint(createDummySession(i++), (int)(5.1 * Point.GETA), (int)(5.1 * Point.GETA));
    points.add(p1);
    for (; i < size; ++i) {
      double d = (double) i;
      points.add(new SphericalPoint(createDummySession(i), (int)((5.0999 + (d / 100000)) * Point.GETA), (int)((5.0999 + (d / 100000)) * Point.GETA)));
    }
    for (int j = 0; j < 5; j++) {
      long start = System.nanoTime();
      VpTree<SphericalPoint> tree = VpTree.buildVpTree(points);
      // System.out.println(tree.toString());
      assertThat(tree.userLocationMap.size(), is(size));
      assertThat(tree.size(), is(size));
      System.out.println("VP-Tree#build(" + size + "): " + ((System.nanoTime() - start) / 1000000d) + "ms");

      start = System.nanoTime();
      List<SphericalPoint> nearbyPoints = tree.findAround(p1.session().sessionId, MessageCommands.areaM(40));
      System.out.println("VP-Tree#findAround: " + ((System.nanoTime() - start) / 1000000d) + "ms");

      for (int k = 1; k < 37; k++) {
        assertTrue("points[" + k + "]" + points.get(k) + ", distance: " + points.get(0).point().distanceMeter(points.get(k).point()), nearbyPoints.contains(points.get(k)));
      }
      assertFalse("points[37]" + points.get(37) + ", distance: " + points.get(0).point().distanceMeter(points.get(37).point()), nearbyPoints.contains(points.get(37)));
    }
  }

  @Test
  public void testNorthPole() {
    VpTree<SphericalPoint> tree = new VpTree<>();
    tree.put(new SphericalPoint(createDummySession(1), Point.forTest(89.9, 70.9)));
    tree.put(new SphericalPoint(createDummySession(2), Point.forTest(89.93109563906, 289.31721675720837)));
    tree.put(new SphericalPoint(createDummySession(3), Point.forTest(89.93109563906, -290.55865037515025)));
    tree.put(new SphericalPoint(createDummySession(4), Point.forTest(-89.93109563906, 70.9)));

    List<SphericalPoint> results = tree.findAround(createDummySession(1).sessionId, MessageCommands.areaKM(16));
    assertThat(results.size(), is(1));
    assertThat(results.get(0).point(), is(Point.forTest(89.93109563906, -290.55865037515025)));
  }

  @Test
  public void testSouthPole() {
    VpTree<SphericalPoint> tree = new VpTree<>();
    tree.put(new SphericalPoint(createDummySession(1), Point.forTest(-89.9, 70.9)));
    tree.put(new SphericalPoint(createDummySession(2), Point.forTest(-89.93109563906, 289.31721675720837)));
    tree.put(new SphericalPoint(createDummySession(3), Point.forTest(-89.93109563906, -290.55865037515025)));
    tree.put(new SphericalPoint(createDummySession(4), Point.forTest(89.93109563906, 70.9)));

    List<SphericalPoint> results = tree.findAround(createDummySession(1).sessionId, MessageCommands.areaKM(16));
    assertThat(results.size(), is(1));
    assertThat(results.get(0).point(), is(Point.forTest(-89.93109563906, -290.55865037515025)));
  }
  @Test
  public void test_findBySessionId() {
    InetSocketAddress sender = DatagramSocketAddress.createUnresolved("localhost", 44445);
    SessionId userA = new SessionId(new byte[]{1, 2, 3, 4, 5, 6, 7, 8});
    SessionId userB = new SessionId(new byte[]{2, 2, 3, 4, 5, 6, 7, 8});

    VpTree<SphericalPoint> tree = new VpTree<>();
    {
      // 0
      SphericalPoint result = tree.findBySessionId(userA);
      assertNull(result);
    }
    {
      // 1
      Point location = Point.forTest(1, 1);
      Session session = Session.create(userA, sender);
      tree.put(new SphericalPoint(session, location));
      SphericalPoint result = tree.findBySessionId(userA);
      assertThat(result.point(), is(location));
      assertThat(result.session(), is(session));
      assertThat(tree.size(), is(1));
    }
    {
      // 2
      Point location = Point.forTest(2, 2);
      Session session = Session.create(userB, sender);
      tree.put(new SphericalPoint(session, location));
      SphericalPoint result = tree.findBySessionId(userB);
      assertThat(result.point(), is(location));
      assertThat(result.session(), is(session));
      assertThat(tree.size(), is(2));
    }
    {
      // Same user as 1
      Point location = Point.forTest(3, 3);
      Session session = Session.create(userA, sender);
      tree.put(new SphericalPoint(session, location));
      SphericalPoint result = tree.findBySessionId(userA);
      assertThat(result.point(), is(location));
      assertThat(result.session(), is(session));
      System.out.println(tree);
      assertThat(tree.size(), is(2));
    }
  }

  @Test
  public void test_findAround() {
    InetSocketAddress sender = DatagramSocketAddress.createUnresolved("localhost", 44445);
    SessionId userA = new SessionId(new byte[]{1, 2, 3, 4, 5, 6, 7, 8});
    Point locationA = Point.forTest(1, 1); // -> 2
    SessionId userB = new SessionId(new byte[]{2, 2, 3, 4, 5, 6, 7, 8});
    Point locationB = Point.forTest(1.3, 1); // -> 2.4
    SessionId userC = new SessionId(new byte[]{3, 2, 3, 4, 5, 6, 7, 8});
    Point locationC = Point.forTest(1.7, 1);
    SessionId userD = new SessionId(new byte[]{4, 2, 3, 4, 5, 6, 7, 8});
    Point locationD = Point.forTest(4, 1);
    SessionId userE = new SessionId(new byte[]{5, 2, 3, 4, 5, 6, 7, 8});
    Point locationE = Point.forTest(3.1, 1);

    VpTree<SphericalPoint> tree = new VpTree<>();
    int km = 80;
    {
      // 0
      List<SphericalPoint> result = tree.findAround(userA, MessageCommands.areaKM(km));
      assertThat(result.size(), is(0));
    }
    {
      // 1
      Session session = Session.create(userA, sender);
      tree.put(new SphericalPoint(session, locationA));
      assertThat(tree.size(), is(1));
      List<SphericalPoint> result = tree.findAround(userA, MessageCommands.areaKM(km));
      assertThat(result.size(), is(0));
    }
    {
      // Set userB within 80km of userA.

      Session session = Session.create(userB, sender);
      tree.put(new SphericalPoint(session, locationB));
      assertThat(tree.size(), is(2));
      List<SphericalPoint> resultA = tree.findAround(userA, MessageCommands.areaKM(km));
      assertThat(resultA.size(), is(1));
      assertThat(resultA.get(0).point(), is(locationB));

      List<SphericalPoint> resultB = tree.findAround(userB, MessageCommands.areaKM(km));
      assertThat(resultB.size(), is(1));
      assertThat(resultB.get(0).point(), is(locationA));
    }
    {
      // Set userB outside of userA.
      locationB = Point.forTest(2.4, 1);
      Session session = Session.create(userB, sender);
      tree.put(new SphericalPoint(session, locationB));
      List<SphericalPoint> result = tree.findAround(userA, MessageCommands.areaKM(km));
      assertThat(result.size(), is(0));
    }
    {
      // Set userC within 80km of userA and userB.
      Session session = Session.create(userC, sender);
      tree.put(new SphericalPoint(session, locationC));
      assertThat(tree.size(), is(3));
      List<SphericalPoint> resultA = tree.findAround(userA, MessageCommands.areaKM(km));
      assertThat(resultA.size(), is(1));
      assertThat(resultA.get(0).point(), is(locationC));
      List<SphericalPoint> resultB = tree.findAround(userB, MessageCommands.areaKM(km));
      assertThat(resultB.size(), is(1));
      assertThat(resultB.get(0).point(), is(locationC));
      List<SphericalPoint> resultC = tree.findAround(userC, MessageCommands.areaKM(km));
      assertThat(resultC.size(), is(2));
      assertThat(resultC.get(0).point(), is(locationA));
      assertThat(resultC.get(1).point(), is(locationB));
    }
    {
      // Move userA within 80km of userB and userC.
      // C < A < B
      Session session = Session.create(userA, sender);
      locationA = Point.forTest(2, 1);
      tree.put(new SphericalPoint(session, locationA));
      assertThat(tree.size(), is(3));
      List<SphericalPoint> resultA = tree.findAround(userA, MessageCommands.areaKM(km));
      assertThat(resultA.size(), is(2));
      assertThat(resultA.get(0).point(), is(locationB));
      assertThat(resultA.get(1).point(), is(locationC));
      List<SphericalPoint> resultB = tree.findAround(userB, MessageCommands.areaKM(km));
      assertThat(resultB.size(), is(2));
      assertThat(resultB.get(0).point(), is(locationC));
      assertThat(resultB.get(1).point(), is(locationA));
      List<SphericalPoint> resultC = tree.findAround(userC, MessageCommands.areaKM(km));
      assertThat(resultC.size(), is(2));
      assertThat(resultC.get(0).point(), is(locationB));
      assertThat(resultC.get(1).point(), is(locationA));
    }
    {
      // Set userD outside of others.
      // C < A < B < D
      Session session = Session.create(userD, sender);
      tree.put(new SphericalPoint(session, locationD));
      assertThat(tree.size(), is(4));
      List<SphericalPoint> resultA = tree.findAround(userA, MessageCommands.areaKM(km));
      assertThat(resultA.size(), is(2));
      assertThat(resultA.get(0).point(), is(locationB));
      assertThat(resultA.get(1).point(), is(locationC));
      List<SphericalPoint> resultB = tree.findAround(userB, MessageCommands.areaKM(km));
      assertThat(resultB.size(), is(2));
      assertThat(resultB.get(0).point(), is(locationC));
      assertThat(resultB.get(1).point(), is(locationA));
      List<SphericalPoint> resultC = tree.findAround(userC, MessageCommands.areaKM(km));
      assertThat(resultC.size(), is(2));
      assertThat(resultC.get(0).point(), is(locationB));
      assertThat(resultC.get(1).point(), is(locationA));
      List<SphericalPoint> result = tree.findAround(userD, MessageCommands.areaKM(km));
      assertThat(result.size(), is(0));
    }
    {
      // Set userE between B and D.
      // C < A < B < E < D
      Session session = Session.create(userE, sender);
      tree.put(new SphericalPoint(session, locationE));
      assertThat(tree.size(), is(5));
      List<SphericalPoint> resultA = tree.findAround(userA, MessageCommands.areaKM(km));
      assertThat(resultA.size(), is(2));
      assertThat(resultA.get(0).point(), is(locationB));
      assertThat(resultA.get(1).point(), is(locationC));
      List<SphericalPoint> resultB = tree.findAround(userB, MessageCommands.areaKM(km));
      assertThat(resultB.size(), is(3));
      assertThat(resultB.get(0).point(), is(locationC));
      assertThat(resultB.get(1).point(), is(locationA));
      assertThat(resultB.get(2).point(), is(locationE));
      List<SphericalPoint> resultC = tree.findAround(userC, MessageCommands.areaKM(km));
      assertThat(resultC.size(), is(2));
      assertThat(resultC.get(0).point(), is(locationB));
      assertThat(resultC.get(1).point(), is(locationA));
      List<SphericalPoint> result = tree.findAround(userD, MessageCommands.areaKM(km));
      assertThat(result.size(), is(0));
    }
  }
}
