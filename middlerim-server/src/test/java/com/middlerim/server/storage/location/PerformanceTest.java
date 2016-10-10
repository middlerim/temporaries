package com.middlerim.server.storage.location;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.middlerim.location.Point;
import com.middlerim.server.MessageCommands;
import com.middlerim.session.Session;

public class PerformanceTest {

  private static final int SIZE = 5_000_000;
  private static final byte AREA = MessageCommands.areaKM(32);

  @Test
  public void testPerformance() {
    List<Point> points = new ArrayList<Point>(SIZE);
    points.add(Point.forTest(35.4, 139.45));
    for (int i = 1; i < SIZE; ++i) {
      points.add(Point.forTest((Math.random() * i) % 90, (Math.random() * i) % 180));
    }

    Session session = SimpleTreeTest.createDummySession(0);

    long simpleBuildTime = 0;
    long simpleFindTime = 0;
//    {
//      long start = System.currentTimeMillis();
//      SimpleTree tree = new SimpleTree();
//      tree.put(new Entry(session, points.get(0)));
//      for (int i = 1; i < SIZE; ++i) {
//        tree.put(new Entry(SimpleTreeTest.createDummySession(i), points.get(i)));
//      }
//      assertThat(tree.size(), is(SIZE));
//      simpleBuildTime = (System.currentTimeMillis() - start);
//
//      start = System.nanoTime();
//      List<Entry> found = tree.findAround(session.sessionId, AREA);
//      simpleFindTime = System.nanoTime() - start;
//      System.out.println("Simple Tree Found: " + found.size());
//    }
    long vpBulkBuildTime = 0;
    long vpBulkFindTime = 0;
    {
      long start = System.currentTimeMillis();
      List<SphericalPoint> vpPoints = new ArrayList<>(SIZE);
      vpPoints.add(new SphericalPoint(session, points.get(0)));
      for (int i = 1; i < SIZE; ++i) {
        vpPoints.add(new SphericalPoint(SimpleTreeTest.createDummySession(i), points.get(i)));
      }
      VpTree<SphericalPoint> tree = VpTree.buildVpTree(vpPoints);
//      System.out.println(tree.toString());
      assertThat(tree.size(), is(SIZE));
      vpBulkBuildTime = (System.currentTimeMillis() - start);
      start = System.nanoTime();
      List<SphericalPoint> found = tree.findAround(session.sessionId, AREA);
      vpBulkFindTime = System.nanoTime() - start;
      System.out.println("    VP Tree Found: " + found.size());
      if (found.size() == 0) {
        tree.findAround(session.sessionId, AREA);
      }
    }
    long vpDynamicBuildTime = 0;
    long vpDynamicFindTime = 0;
    {
      long start = System.currentTimeMillis();
      VpTree<SphericalPoint> tree = new VpTree<>();
      tree.put(new SphericalPoint(session, points.get(0)));
      for (int i = 1; i < SIZE; ++i) {
        tree.put(new SphericalPoint(SimpleTreeTest.createDummySession(i), points.get(i)));
      }
//      System.out.println(tree.toString());
      assertThat(tree.size(), is(SIZE));
      vpDynamicBuildTime = (System.currentTimeMillis() - start);
      start = System.nanoTime();
      List<SphericalPoint> found = tree.findAround(session.sessionId, AREA);
      vpDynamicFindTime = System.nanoTime() - start;
      System.out.println("    VP Tree Found: " + found.size());
      if (found.size() == 0) {
        tree.findAround(session.sessionId, AREA);
      }
    }
    System.out.println("Build time:");
//    System.out.println("      Simple Tree: " + simpleBuildTime + "ms");
    System.out.println("    VP Tree(bulk): " + vpBulkBuildTime + "ms");
    System.out.println(" VP Tree(Dynamic): " + vpDynamicBuildTime + "ms");
    System.out.println("Find time:");
//    System.out.println("      Simple Tree: " + simpleFindTime / 1000000d + "ms");
    System.out.println("    VP Tree(bulk): " + vpBulkFindTime / 1000000d + "ms");
    System.out.println(" VP Tree(Dynamic): " + vpDynamicFindTime / 1000000d + "ms");
  }
}
