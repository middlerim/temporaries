package com.middlerim.server.message;

import com.middlerim.message.ControlMessage;
import com.middlerim.message.Outbound;
import com.middlerim.server.Headers;
import com.middlerim.session.Session;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.DatagramPacket;

public final class Markers {
  private Markers() {
  }

  public static final Received RECEIVED = new Received();
  public static final InvalidSequence INVALID_SEQUENCE = new InvalidSequence();
  public static final InvalidData INVALID_DATA = new InvalidData();
  public static final InvalidData NOTFOUND = new InvalidData();

  private static final class Received implements Outbound, ControlMessage {

    private static final int FIXED_BYTE_SIZE = 3;

    private Received() {
    }

    @Override
    public ChannelFuture processOutput(ChannelHandlerContext ctx, Session recipient) {
      return ctx.writeAndFlush(new DatagramPacket(ctx.alloc().buffer(FIXED_BYTE_SIZE, FIXED_BYTE_SIZE).writeByte(Headers.RECEIVED).writeShort(recipient.sessionId.sequenceNo()), recipient.address));
    }

    @Override
    public int byteSize() {
      return FIXED_BYTE_SIZE;
    }
  }

  private static final class InvalidSequence implements Outbound, ControlMessage {

    private static final int FIXED_BYTE_SIZE = 3;

    private InvalidSequence() {
    }

    @Override
    public ChannelFuture processOutput(ChannelHandlerContext ctx, Session recipient) {
      return ctx.writeAndFlush(new DatagramPacket(ctx.alloc().buffer(FIXED_BYTE_SIZE, FIXED_BYTE_SIZE).writeByte(Headers.AGAIN).writeShort(recipient.sessionId.sequenceNo()), recipient.address));
    }

    @Override
    public int byteSize() {
      return FIXED_BYTE_SIZE;
    }
  }

  private static final class InvalidData implements Outbound, ControlMessage {

    private static final int FIXED_BYTE_SIZE = 3;

    private InvalidData() {
    }

    @Override
    public ChannelFuture processOutput(ChannelHandlerContext ctx, Session recipient) {
      return ctx.writeAndFlush(new DatagramPacket(ctx.alloc().buffer(FIXED_BYTE_SIZE, FIXED_BYTE_SIZE).writeByte(Headers.AGAIN).writeShort(recipient.sessionId.sequenceNo()), recipient.address));
    }

    @Override
    public int byteSize() {
      return FIXED_BYTE_SIZE;
    }
  }
}