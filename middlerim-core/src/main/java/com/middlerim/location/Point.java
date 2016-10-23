package com.middlerim.location;

public class Point implements Comparable<Point> {
  public static final double GETA = 100000;
  private static final double PI180 = Math.PI / 180 / GETA;

  public final int latitude;
  public final int longitude;

  public static Point convert(double latitude, double longitude) {
    return new Point((int) (latitude * GETA), (int) (longitude * GETA));
  }

  public Coordinate toCoordinate() {
    return new Coordinate(latitude / GETA, longitude / GETA);
  }

  public Point(int latitude, int longitude) {
    this.latitude = latitude;
    this.longitude = longitude;
  }

  public static double radians(int deg) {
    return deg * PI180;
  }
  public int distanceSimple(Point b) {
    return Math.abs(latitude - b.latitude) + Math.abs(longitude - b.longitude);
  }

  public int distanceMeter(Point b) {
    double lat1 = radians(latitude);
    double lat2 = radians(b.latitude);
    int distanceMeter = (int) (6378137d
        * Math.acos(Math.cos(lat1) * Math.cos(lat2)
            * Math.cos(radians(b.longitude) - radians(longitude))
            + Math.sin(lat1) * Math.sin(lat2)));
    return distanceMeter;
  }

  @Override
  public String toString() {
    return "(" + (latitude / GETA) + ", " + (longitude / GETA) + ")";
  }

  @Override
  public int hashCode() {
    return (int) latitude;
  }

  @Override
  public int compareTo(Point o) {
    int l = latitude - o.latitude;
    if (l == 0) {
      return o.longitude - o.longitude;
    }
    return l;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null) {
      return false;
    }
    Point b = (Point) obj;
    return b.latitude == latitude
        && b.longitude == longitude;
  }
}
