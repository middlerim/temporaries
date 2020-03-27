package com.middlerim.server.command.message;

import java.nio.ByteBuffer;

import com.middlerim.location.Point;
import com.middlerim.message.Inbound;
import com.middlerim.server.command.storage.Locations;
import com.middlerim.session.Session;
import com.middlerim.storage.persistent.Persistent;

import io.netty.channel.ChannelHandlerContext;

public class Location implements Inbound, Persistent<Location> {
  public static final int SERIALIZED_BYTE_SIZE = Byte.BYTES + Integer.BYTES * 2;

  private Session session;
  public Point point;

  public static Location createEmpty(Session session) {
    Location location = new Location();
    location.session = session;
    return location;
  }

  private Location() {
  }

  public Location(Session session, Point point) {
    this.session = session;
    this.point = point;
  }

  @Override
  public void processInput(ChannelHandlerContext ctx) {
    Locations.updateLocation(session, this);
  }

  @Override
  public long id() {
    return session.sessionId.userId();
  }

  @Override
  public void read(ByteBuffer buf) {
    buf.put(session.sessionId.clientSequenceNo())
        .putInt(point.latitude)
        .putInt(point.longitude);
  }

  @Override
  public void write(ByteBuffer buf) {
    buf.position(1);
    point = new Point(buf.getInt(), buf.getInt());
  }

  @Override
  public boolean equals(Object obj) {
    return point.equals(((Location) obj).point);
  }
  @Override
  public String toString() {
    return point.toString();
  }
}
