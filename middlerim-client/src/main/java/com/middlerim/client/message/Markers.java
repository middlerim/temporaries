package com.middlerim.client.message;

import com.middlerim.client.CentralServer;
import com.middlerim.message.ControlMessage;
import com.middlerim.message.Inbound;
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
  public static final Exit EXIT = new Exit();
  public static final AssignAID ASSIGN_AID = new AssignAID();

  private static final class Received implements Outbound, ControlMessage {
    private static final int FIXED_BYTE_SIZE = 9;

    private Received() {
    }

    @Override
    public ChannelFuture processOutput(ChannelHandlerContext ctx, Session recipient) {
      byte[] sessionId = new byte[8];
      recipient.sessionId.readBytes(sessionId);
      return ctx.writeAndFlush(new DatagramPacket(ctx.alloc().buffer(FIXED_BYTE_SIZE, FIXED_BYTE_SIZE).writeByte(Headers.RECEIVED).writeBytes(sessionId), recipient.address));
    }

    @Override
    public int byteSize() {
      return FIXED_BYTE_SIZE;
    }
  }

  private static final class InvalidSequence implements Outbound, ControlMessage {
    private static final int FIXED_BYTE_SIZE = 9;

    private InvalidSequence() {
    }

    @Override
    public ChannelFuture processOutput(ChannelHandlerContext ctx, Session recipient) {
      byte[] sessionId = new byte[8];
      recipient.sessionId.readBytes(sessionId);
      return ctx.writeAndFlush(new DatagramPacket(ctx.alloc().buffer(FIXED_BYTE_SIZE, FIXED_BYTE_SIZE).writeByte(Headers.AGAIN).writeBytes(sessionId), recipient.address));
    }

    @Override
    public int byteSize() {
      return FIXED_BYTE_SIZE;
    }
  }

  private static final class InvalidData implements Outbound, ControlMessage {
    private static final int FIXED_BYTE_SIZE = 9;

    private InvalidData() {
    }

    @Override
    public ChannelFuture processOutput(ChannelHandlerContext ctx, Session recipient) {
      byte[] sessionId = new byte[8];
      recipient.sessionId.readBytes(sessionId);
      return ctx.writeAndFlush(new DatagramPacket(ctx.alloc().buffer(FIXED_BYTE_SIZE, FIXED_BYTE_SIZE).writeByte(Headers.AGAIN).writeBytes(sessionId), recipient.address));
    }

    @Override
    public int byteSize() {
      return FIXED_BYTE_SIZE;
    }
  }

  public static final class Exit implements Outbound, ControlMessage {
    private static final int FIXED_BYTE_SIZE = 9;

    private Exit() {
    }

    @Override
    public ChannelFuture processOutput(ChannelHandlerContext ctx, Session session) {
      byte[] sessionId = new byte[8];
      session.sessionId.readBytes(sessionId);
      return ctx.write(new DatagramPacket(ctx.alloc().buffer(FIXED_BYTE_SIZE, FIXED_BYTE_SIZE).writeByte(Headers.EXIT).writeBytes(sessionId), CentralServer.serverAddress));
    }

    @Override
    public int byteSize() {
      return FIXED_BYTE_SIZE;
    }
  }

  private static final class AssignAID implements Inbound, Outbound, ControlMessage {

    private static final int FIXED_BYTE_SIZE = 1;
    private static boolean called = false;
    private AssignAID() {
    }

    @Override
    public void processInput(ChannelHandlerContext ctx) {
      // 
      if (called) {
        throw new IllegalStateException("Assign Anonymous ID response must be called only once.");
      }
      called = true;
      CentralServer.postCreateServer(ctx.channel());
    }

    @Override
    public ChannelFuture processOutput(ChannelHandlerContext ctx, Session recipient) {
      return ctx.writeAndFlush(new DatagramPacket(ctx.alloc().buffer(FIXED_BYTE_SIZE, FIXED_BYTE_SIZE).writeByte(Headers.ASSIGN_AID), recipient.address));
    }

    @Override
    public int byteSize() {
      return FIXED_BYTE_SIZE;
    }
  }
}
