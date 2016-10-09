package com.middlerim.location;

public class Point implements Comparable<Point> {
  private static final double PI180 = Math.PI / 180;
  private static final double ACCEPTABLE_RANGE = 0.00001;
  private static final double ACCEPTABLE_RANGE_N = -0.00001;

  public final double latitude;
  public final double longitude;

  public Point(double latitude, double longtitude) {
    this.latitude = latitude;
    this.longitude = longtitude;
  }

  private double radians(double deg) {
    return deg * PI180;
  }
  public int distanceSimple(Point b) {
    return (int) ((Math.abs(latitude - b.latitude) + Math.abs(longitude - b.longitude)) * 10000);
  }

  public int distanceMeter(Point b) {
    double lat1 = radians(latitude);
    double lat2 = radians(b.latitude);
    int distanceMeter = (int) (6378137d * Math.acos(Math.cos(lat1) *
        Math.cos(lat2) * Math.cos(radians(b.longitude) - radians(longitude)) +
        Math.sin(lat1) * Math.sin(lat2)));
    return distanceMeter;
  }

  @Override
  public String toString() {
    return "(" + latitude + ", " + longitude + ")";
  }

  @Override
  public int hashCode() {
    return (int) latitude;
  }

  public static int compare(double a, double b) {
    double d = a - b;
    if (ACCEPTABLE_RANGE_N <= d && d <= ACCEPTABLE_RANGE) {
      return 0;
    }
    return d < 0 ? -1 : 1;
  }

  @Override
  public int compareTo(Point o) {
    int l = compare(o.latitude, latitude);
    if (l == 0) {
      return longitude < o.longitude ? -1 : 1;
    }
    return l;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null) {
      return false;
    }
    Point b = (Point) obj;
    return compare(b.latitude, latitude) == 0
        && compare(b.longitude, longitude) == 0;
  }
}
