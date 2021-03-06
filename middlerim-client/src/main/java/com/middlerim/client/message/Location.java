package com.middlerim.client.message;

import com.middlerim.location.Point;
import com.middlerim.message.Outbound;
import com.middlerim.server.Headers;
import com.middlerim.session.Session;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.DatagramPacket;

public final class Location implements Outbound {
  public static final int FIXED_BYTE_SIZE = 17;
  private final Point point;

  public Location(Point point) {
    this.point = point;
  }

  @Override
  public ChannelFuture processOutput(ChannelHandlerContext ctx, Session session) {
    ByteBuf buf = ctx.alloc().buffer(FIXED_BYTE_SIZE, FIXED_BYTE_SIZE);
    byte[] sessionIdBytes = new byte[8];
    session.sessionId.readBytes(sessionIdBytes);
    buf.writeByte(Headers.Command.LOCATION.code)
        .writeBytes(sessionIdBytes)
        .writeInt(point.latitude)
        .writeInt(point.longitude);
    return ctx.write(new DatagramPacket(buf, session.address));
  }
  @Override
  public String toString() {
    return point.toString();
  }

  @Override
  public int byteSize() {
    return FIXED_BYTE_SIZE;
  }
}
