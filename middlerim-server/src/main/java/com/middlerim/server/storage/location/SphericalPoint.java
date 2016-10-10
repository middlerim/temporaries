package com.middlerim.server.storage.location;

import com.middlerim.location.Point;
import com.middlerim.session.Session;

public class SphericalPoint implements TreePoint<SphericalPoint> {
  private final Session session;
  private final Point point;

  public SphericalPoint(Session session, Point point) {
    this.session = session;
    this.point = point;
  }
  public SphericalPoint(Session session, int latitude, int longtitude) {
    this(session, new Point(latitude, longtitude));
  }

  @Override
  public Session session() {
    return session;
  }
  @Override
  public Point point() {
    return point;
  }

  @Override
  public String toString() {
    return point.toString();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    return session.sessionId.equals(((SphericalPoint) o).session().sessionId);
  }
}
