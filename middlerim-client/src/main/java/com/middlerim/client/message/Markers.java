package com.middlerim.client.message;

import com.middlerim.message.Inbound;
import com.middlerim.message.Outbound;
import com.middlerim.message.SequentialMessage;
import com.middlerim.server.Headers;
import com.middlerim.session.Session;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.DatagramPacket;

public final class Markers {
  private Markers() {
  }

  public static final InvalidData INVALID_DATA = new InvalidData();
  public static final Exit EXIT = new Exit();
  public static final AssignAID ASSIGN_AID = new AssignAID();

  public static final class Again implements Inbound, Outbound {
    public final int tag;
    public Again(int tag) {
      this.tag = tag;
    }

    @Override
    public void processInput(ChannelHandlerContext ctx) {
      ctx.channel().write(this);
    }

    @Override
    public int byteSize() {
      return 0;
    }

    @Override
    public ChannelFuture processOutput(ChannelHandlerContext ctx, Session recipient) {
      throw new UnsupportedOperationException("Don't call it. This is just a marker.");
    }
  }

  public static final class Received implements Outbound {
    private static final int FIXED_BYTE_SIZE = 9;

    private final short serverSequenceNo;
    public Received(short serverSequenceNo) {
      this.serverSequenceNo = serverSequenceNo;
    }

    @Override
    public ChannelFuture processOutput(ChannelHandlerContext ctx, Session recipient) {
      byte[] sessionId = new byte[8];
      recipient.sessionId.copyWithNewServerSequenceNo(serverSequenceNo).readBytes(sessionId);
      return ctx.writeAndFlush(new DatagramPacket(ctx.alloc().buffer(FIXED_BYTE_SIZE, FIXED_BYTE_SIZE).writeByte(Headers.RECEIVED).writeBytes(sessionId), recipient.address));
    }

    @Override
    public int byteSize() {
      return FIXED_BYTE_SIZE;
    }
  }

  private static final class InvalidData implements Outbound {
    private static final int FIXED_BYTE_SIZE = 9;

    private InvalidData() {
    }

    @Override
    public ChannelFuture processOutput(ChannelHandlerContext ctx, Session recipient) {
      byte[] sessionId = new byte[8];
      recipient.sessionId.readBytes(sessionId);
      return ctx.writeAndFlush(new DatagramPacket(ctx.alloc().buffer(FIXED_BYTE_SIZE, FIXED_BYTE_SIZE).writeByte(Headers.ERROR).writeBytes(sessionId), recipient.address));
    }

    @Override
    public int byteSize() {
      return FIXED_BYTE_SIZE;
    }
  }

  private static final class Exit implements Outbound {
    private static final int FIXED_BYTE_SIZE = 9;

    private Exit() {
    }

    @Override
    public ChannelFuture processOutput(ChannelHandlerContext ctx, Session session) {
      byte[] sessionId = new byte[8];
      session.sessionId.readBytes(sessionId);
      return ctx.write(new DatagramPacket(ctx.alloc().buffer(FIXED_BYTE_SIZE, FIXED_BYTE_SIZE).writeByte(Headers.EXIT).writeBytes(sessionId), session.address));
    }

    @Override
    public int byteSize() {
      return FIXED_BYTE_SIZE;
    }
  }

  static final class AssignAID implements Outbound, SequentialMessage {
    private static final int FIXED_BYTE_SIZE = 1;

    private AssignAID() {
    }

    @Override
    public ChannelFuture processOutput(ChannelHandlerContext ctx, Session recipient) {
      return ctx.writeAndFlush(new DatagramPacket(ctx.alloc().buffer(FIXED_BYTE_SIZE, FIXED_BYTE_SIZE).writeByte(Headers.ASSIGN_AID), recipient.address));
    }

    @Override
    public int byteSize() {
      return FIXED_BYTE_SIZE;
    }

    @Override
    public int tag() {
      return -1;
    }
  }
}
