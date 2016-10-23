package com.middlerim.location;

public final class Coordinate {

  public final double latitude;
  public final double longitude;

  public Coordinate(double latitude, double longitude) {
    this.latitude = latitude;
    this.longitude = longitude;
  }

  public Point toPoint() {
    return Point.convert(latitude, longitude);
  }

  @Override
  public String toString() {
    return "(" + latitude + ", " + longitude + ")";
  }
}
