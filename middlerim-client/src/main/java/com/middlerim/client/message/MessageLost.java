package com.middlerim.client.message;

import com.middlerim.client.Config;
import com.middlerim.message.Outbound;
import com.middlerim.server.Headers;
import com.middlerim.session.Session;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.DatagramPacket;

public class MessageLost implements Outbound {
  private static final int FIXED_BYTE_SIZE = 17;

  private final short current;
  private final short server;
  public MessageLost(short current, short server) {
    this.current = current;
    this.server = server;
  }

  @Override
  public ChannelFuture processOutput(ChannelHandlerContext ctx, Session recipient) {
    // FIXME DO test!!
    if (current > server) {
      // The client has newer messages already.
      return ctx.newSucceededFuture();
    }
    short from = current;
    if (current + Config.ABANDONED_MESSAGE_THREASHOLD < server) {
      from = (short) (server - Config.ABANDONED_MESSAGE_THREASHOLD);
    }
    byte[] sessionId = new byte[8];
    recipient.sessionId.readBytes(sessionId);
    return ctx.writeAndFlush(new DatagramPacket(ctx.alloc().buffer(FIXED_BYTE_SIZE, FIXED_BYTE_SIZE)
        .writeByte(Headers.AGAIN)
        .writeBytes(sessionId)
        .writeShort(from)
        .writeShort(server), recipient.address));
  }

  @Override
  public int byteSize() {
    return FIXED_BYTE_SIZE;
  }
}
