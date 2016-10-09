package com.middlerim.server.message;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.middlerim.location.Point;
import com.middlerim.message.Inbound;
import com.middlerim.server.channel.AttributeKeys;
import com.middlerim.server.storage.Locations;
import com.middlerim.session.Session;

import io.netty.channel.ChannelHandlerContext;

public class Location implements Inbound {
  private static final Logger logger = LoggerFactory.getLogger(Location.class);

  public final Point point;
  public Location(Point point) {
    this.point = point;
  }

  @Override
  public void processInput(ChannelHandlerContext ctx) {
    Session session = ctx.channel().attr(AttributeKeys.SESSION).get();
    Locations.updateLocation(session, this.point);
    if (logger.isDebugEnabled()) {
      logger.debug("Updated location: " + session + point);
    }
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
