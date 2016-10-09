package com.middlerim.location;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import org.junit.Test;

public class PointTest {

  @Test
  public void testWithIn() {
    Point a = new Point(1, 1);
    Point b = new Point(1, 1);
    assertTrue(a.distanceMeter(b) == 0);

    a = new Point(-1d, -1d);
    b = new Point(-1d, -1d);
    assertTrue(a.distanceMeter(b) == 0);

    a = new Point(0d, 0d);
    b = new Point(0d, 0d);
    assertTrue(a.distanceMeter(b) == 0);

    a = new Point(-1d, -1d);
    b = new Point(-1.1d, -1.1d);
    assertThat(a.distanceMeter(b), is(157_41));

    a = new Point(0, 0);
    b = new Point(0.705, 0.705);
    assertThat(a.distanceMeter(b), is(110_986));

    b = new Point(-0.7, -0.7);
    assertThat(a.distanceMeter(b), is(110_199));

    a = new Point(-1, -1);
    b = new Point(-0.3, -0.3);
    assertThat(a.distanceMeter(b), is(110_196));

    a = new Point(-1, -1);
    b = new Point(-0.29, -0.29);
    assertThat(a.distanceMeter(b), is(111_771));

    a = new Point(-359.343292844065, -359.6588680149514);
    b = new Point(90.2, 70.9);
    assertThat(a.distanceMeter(b), is(9953060));

    a = new Point(5.10026, 5.10026);
    b = new Point(5.1, 5.1);
    assertThat(a.distanceMeter(b), is(40));
  }

  @Test
  public void testDistanceSimple() {
    Point a = new Point(1.1d, 1.1d);
    Point b = new Point(1.1d, 1.1d);
    assertThat(a.distanceSimple(b), is(0));

    a = new Point(1.1d, 1.1d);
    b = new Point(-1.1d, -1.1d);
    assertThat(a.distanceSimple(b), is(44000));

    a = new Point(1.1d, 1.1d);
    b = new Point(2.1d, 2.1d);
    assertThat(a.distanceSimple(b), is(20000));

    a = new Point(-1d, -1d);
    b = new Point(1.1d, 1.1d);
    assertThat(a.distanceSimple(b), is(42000));

    a = new Point(-1.1d, -1.1d);
    b = new Point(-2.2d, -2.2d);
    assertThat(a.distanceSimple(b), is(22000));

    a = new Point(1.111d, -1.111d);
    b = new Point(-1.111d, 1.111d);
    assertThat(a.distanceSimple(b), is(44440));

    a = new Point(-1.111d, 1.111d);
    b = new Point(1.111d, -1.111d);
    assertThat(a.distanceSimple(b), is(44440));
  }

  @Test
  public void testDistanceMeterSimple() {
    double d3 = 0;
    int d4 = 0;
    for (int i = 0; i < 1000; ++i) {
      Point a = new Point(((Math.random() * i) % 180) - 90.1, ((Math.random() * i) % 360) - 180.1);
      Point b = new Point(((Math.random() * i) % 180) - 90.1, ((Math.random() * i) % 360) - 180.1);
      double d1 = a.distanceSimple(b);
      int d2 = a.distanceMeter(b);
      if (i == 0) {
        continue;
      }
      assertTrue(i + ":" + d1 + ", " + d2 + ", " + d3 + ", " + d4, ((d1 - d3) < 0 && (d2 - d4) < 0) || ((d1 - d3) >= 0 && (d2 - d4) >= 0));
      d3 = d1;
      d4 = d2;
    }

  }
}
