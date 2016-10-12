package com.middlerim.location;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import org.junit.Ignore;
import org.junit.Test;

public class PointTest {

  @Test
  public void testWithIn() {
    Point a = Point.convert(1, 1);
    Point b = Point.convert(1, 1);
    assertTrue(a.distanceMeter(b) == 0);

    a = Point.convert(-1d, -1d);
    b = Point.convert(-1d, -1d);
    assertTrue(a.distanceMeter(b) == 0);

    a = Point.convert(0d, 0d);
    b = Point.convert(0d, 0d);
    assertTrue(a.distanceMeter(b) == 0);

    a = Point.convert(-1d, -1d);
    b = Point.convert(-1.1d, -1.1d);
    assertThat(a.distanceMeter(b), is(157_41));

    a = Point.convert(0d, 0d);
    b = Point.convert(0.705, 0.705);
    assertThat(a.distanceMeter(b), is(110_986));

    b = Point.convert(-0.7, -0.7);
    assertThat(a.distanceMeter(b), is(110_199));

    a = Point.convert(-1d, -1d);
    b = Point.convert(-0.3d, -0.3d);
    assertThat(a.distanceMeter(b), is(110_196));

    a = Point.convert(-1d, -1d);
    b = Point.convert(-0.29, -0.29);
    assertThat(a.distanceMeter(b), is(111_772));

    a = Point.convert(-359.343292844065, -359.6588680149514);
    b = Point.convert(90.2, 70.9);
    assertThat(a.distanceMeter(b), is(9953060));

    a = Point.convert(5.10026, 5.10026);
    b = Point.convert(5.1, 5.1);
    assertThat(a.distanceMeter(b), is(40));
  }

  @Test
  public void testDistanceSimple() {
    Point a = Point.convert(1.1d, 1.1d);
    Point b = Point.convert(1.1d, 1.1d);
    assertThat(a.distanceSimple(b), is(0));

    a = Point.convert(1.1d, 1.1d);
    b = Point.convert(-1.1d, -1.1d);
    assertThat(a.distanceSimple(b), is(440000));

    a = Point.convert(1.1d, 1.1d);
    b = Point.convert(2.1d, 2.1d);
    assertThat(a.distanceSimple(b), is(200000));

    a = Point.convert(-1d, -1d);
    b = Point.convert(1.1d, 1.1d);
    assertThat(a.distanceSimple(b), is(420000));

    a = Point.convert(-1.1d, -1.1d);
    b = Point.convert(-2.2d, -2.2d);
    assertThat(a.distanceSimple(b), is(220000));

    a = Point.convert(1.111d, -1.111d);
    b = Point.convert(-1.111d, 1.111d);
    assertThat(a.distanceSimple(b), is(444400));

    a = Point.convert(-1.111d, 1.111d);
    b = Point.convert(1.111d, -1.111d);
    assertThat(a.distanceSimple(b), is(444400));
  }

  private double rad(double deg) {
    return deg * Math.PI / 180;
  }

  @Test
  public void testRadians() {
    Point a = Point.convert(1d, 1d);
    assertThat(Point.radians(a.latitude), is(rad(1)));
  }

  @Ignore
  @Test
  public void testDistanceMeterSimple() {
    double d3 = 0;
    int d4 = 0;
    for (int i = 0; i < 1000; ++i) {
      Point a = Point.convert(((Math.random() * i) % 180) - 90.1, ((Math.random() * i) % 360) - 180.1);
      Point b = Point.convert(((Math.random() * i) % 180) - 90.1, ((Math.random() * i) % 360) - 180.1);
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
