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
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    Coordinate b = (Coordinate) obj;
    return (int) (latitude * Point.GETA) == (int) (b.latitude * Point.GETA)
        && (int) (longitude * Point.GETA) == (int) (b.longitude * Point.GETA);
  }

  @Override
  public String toString() {
    return "(" + latitude + ", " + longitude + ")";
  }
}
