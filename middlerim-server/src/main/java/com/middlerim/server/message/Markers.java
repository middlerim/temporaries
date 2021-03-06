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

  public static final InvalidData INVALID_DATA = new InvalidData();
  public static final InvalidData NOTFOUND = new InvalidData();
  public static final AssignAID ASSIGN_AID = new AssignAID();
  public static final UpdateAID UPDATE_AID = new UpdateAID();

  public static class Received implements Outbound {

    private static final int FIXED_BYTE_SIZE = 5;

    private final int tag;
    public Received(int tag) {
      this.tag = tag;
    }

    @Override
    public ChannelFuture processOutput(ChannelHandlerContext ctx, Session recipient) {
      return ctx.writeAndFlush(new DatagramPacket(ctx.alloc().buffer(FIXED_BYTE_SIZE, FIXED_BYTE_SIZE).writeByte(Headers.Control.RECEIVED.code).writeInt(tag), recipient.address));
    }

    @Override
    public int byteSize() {
      return FIXED_BYTE_SIZE;
    }
  }

  public static final class InvalidSequence implements Outbound {

    private static final int FIXED_BYTE_SIZE = 6;

    private final int tag;
    public InvalidSequence(int tag) {
      this.tag = tag;
    }

    @Override
    public ChannelFuture processOutput(ChannelHandlerContext ctx, Session recipient) {
      return ctx.writeAndFlush(new DatagramPacket(ctx.alloc().buffer(FIXED_BYTE_SIZE, FIXED_BYTE_SIZE)
          .writeByte(Headers.Control.AGAIN.code)
          .writeByte(recipient.sessionId.clientSequenceNo())
          .writeInt(tag), recipient.address));
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
      return ctx.writeAndFlush(new DatagramPacket(ctx.alloc().buffer(FIXED_BYTE_SIZE, FIXED_BYTE_SIZE).writeByte(Headers.Control.ERROR.code), recipient.address));
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
      return ctx.writeAndFlush(new DatagramPacket(ctx.alloc().buffer(FIXED_BYTE_SIZE, FIXED_BYTE_SIZE).writeByte(Headers.Control.ASSIGN_AID.code).writeBytes(sessionId), recipient.address));
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
      return ctx.writeAndFlush(new DatagramPacket(ctx.alloc().buffer(FIXED_BYTE_SIZE, FIXED_BYTE_SIZE).writeByte(Headers.Control.AGAIN.code).writeBytes(userId), recipient.address));
    }

    @Override
    public int byteSize() {
      return FIXED_BYTE_SIZE;
    }
  }
}
