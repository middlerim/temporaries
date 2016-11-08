package com.middlerim.server.command.message;

import java.nio.ByteBuffer;

import com.middlerim.location.Point;
import com.middlerim.message.Inbound;
import com.middlerim.server.command.channel.AttributeKeys;
import com.middlerim.server.command.storage.Locations;
import com.middlerim.session.Session;
import com.middlerim.session.SessionId;
import com.middlerim.storage.persistent.Persistent;

import io.netty.channel.ChannelHandlerContext;

public class Location implements Inbound, Persistent<Location> {
  public static final int SERIALIZED_BYTE_SIZE = Byte.BYTES + Integer.BYTES * 2;

  private SessionId sessionId;
  public Point point;

  public static Location createEmpty(SessionId sessionId) {
    Location location = new Location();
    location.sessionId = sessionId;
    return location;
  }

  private Location() {
  }

  public Location(SessionId sessionId, Point point) {
    this.sessionId = sessionId;
    this.point = point;
  }

  @Override
  public void processInput(ChannelHandlerContext ctx) {
    Session session = ctx.channel().attr(AttributeKeys.SESSION).get();
    sessionId = session.sessionId;
    Locations.updateLocation(session, this);
  }

  @Override
  public long id() {
    return sessionId.userId();
  }

  @Override
  public void read(ByteBuffer buf) {
    buf.put(sessionId.clientSequenceNo())
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
