package com.middlerim.server.message;

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
  public static final AssignAID ASSIGN_AID = new AssignAID();
  public static final UpdateAID UPDATE_AID = new UpdateAID();

  private static class Received implements Outbound {

    private static final int FIXED_BYTE_SIZE = 2;

    private Received() {
    }

    @Override
    public ChannelFuture processOutput(ChannelHandlerContext ctx, Session recipient) {
      return ctx.writeAndFlush(new DatagramPacket(ctx.alloc().buffer(FIXED_BYTE_SIZE, FIXED_BYTE_SIZE).writeByte(Headers.RECEIVED).writeByte(recipient.sessionId.clientSequenceNo()), recipient.address));
    }

    @Override
    public int byteSize() {
      return FIXED_BYTE_SIZE;
    }
  }

  private static final class InvalidSequence implements Outbound {

    private static final int FIXED_BYTE_SIZE = 2;

    private InvalidSequence() {
    }

    @Override
    public ChannelFuture processOutput(ChannelHandlerContext ctx, Session recipient) {
      return ctx.writeAndFlush(new DatagramPacket(ctx.alloc().buffer(FIXED_BYTE_SIZE, FIXED_BYTE_SIZE).writeByte(Headers.AGAIN).writeByte(recipient.sessionId.clientSequenceNo()), recipient.address));
    }

    @Override
    public int byteSize() {
      return FIXED_BYTE_SIZE;
    }
  }

  private static final class InvalidData implements Outbound {

    private static final int FIXED_BYTE_SIZE = 1;

    private InvalidData() {
    }

    @Override
    public ChannelFuture processOutput(ChannelHandlerContext ctx, Session recipient) {
      return ctx.writeAndFlush(new DatagramPacket(ctx.alloc().buffer(FIXED_BYTE_SIZE, FIXED_BYTE_SIZE).writeByte(Headers.ERROR), recipient.address));
    }

    @Override
    public int byteSize() {
      return FIXED_BYTE_SIZE;
    }
  }

  private static final class AssignAID implements Outbound {

    private static final int FIXED_BYTE_SIZE = 9;

    private AssignAID() {
    }

    @Override
    public ChannelFuture processOutput(ChannelHandlerContext ctx, Session recipient) {
      byte[] sessionId = new byte[8];
      recipient.sessionId.readBytes(sessionId);
      return ctx.writeAndFlush(new DatagramPacket(ctx.alloc().buffer(FIXED_BYTE_SIZE, FIXED_BYTE_SIZE).writeByte(Headers.ASSIGN_AID).writeBytes(sessionId), recipient.address));
    }

    @Override
    public int byteSize() {
      return FIXED_BYTE_SIZE;
    }
  }
  
  private static final class UpdateAID implements Outbound {

    private static final int FIXED_BYTE_SIZE = 5;

    private UpdateAID() {
    }

    @Override
    public ChannelFuture processOutput(ChannelHandlerContext ctx, Session recipient) {
      byte[] userId = new byte[4];
      recipient.sessionId.readUserIdBytes(userId);
      return ctx.writeAndFlush(new DatagramPacket(ctx.alloc().buffer(FIXED_BYTE_SIZE, FIXED_BYTE_SIZE).writeByte(Headers.UPDATE_AID).writeBytes(userId), recipient.address));
    }

    @Override
    public int byteSize() {
      return FIXED_BYTE_SIZE;
    }
  }
}
