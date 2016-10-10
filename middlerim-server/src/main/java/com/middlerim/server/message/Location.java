package com.middlerim.server.message;

import java.nio.ByteBuffer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.middlerim.location.Point;
import com.middlerim.message.Inbound;
import com.middlerim.server.channel.AttributeKeys;
import com.middlerim.server.storage.Locations;
import com.middlerim.server.storage.persistent.Persistent;
import com.middlerim.session.Session;
import com.middlerim.session.SessionId;

import io.netty.channel.ChannelHandlerContext;

public class Location implements Inbound, Persistent {
  private static final Logger logger = LoggerFactory.getLogger(Location.class);

  public static final int SERIALIZED_BYTE_SIZE = Short.BYTES + Integer.BYTES * 2;

  private SessionId sessionId;
  public final Point point;

  public Location(Point point) {
    this.point = point;
  }

  @Override
  public void processInput(ChannelHandlerContext ctx) {
    Session session = ctx.channel().attr(AttributeKeys.SESSION).get();
    sessionId = session.sessionId;
    Locations.updateLocation(session, this);
    if (logger.isDebugEnabled()) {
      logger.debug("Updated location: " + session + point);
    }
  }

  @Override
  public long id() {
    return sessionId.userId();
  }

  @Override
  public void read(ByteBuffer buf) {
    buf.putShort(sessionId.sequenceNo())
        .putInt(point.latitude)
        .putInt(point.longitude);
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
