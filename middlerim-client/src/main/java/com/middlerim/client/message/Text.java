package com.middlerim.client.message;

import java.nio.ByteBuffer;

import com.middlerim.client.CentralEvents;
import com.middlerim.client.Config;
import com.middlerim.location.Coordinate;
import com.middlerim.message.Inbound;
import com.middlerim.message.Outbound;
import com.middlerim.message.SequentialMessage;
import com.middlerim.server.Headers;
import com.middlerim.session.Session;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.DatagramPacket;

public class Text {

  public static class In implements Inbound {

    public final long userId;
    public final Coordinate location;
    public final int displayNameLength;
    public final byte messageCommand;
    public final ByteBuffer data;

    public In(long userId, Coordinate location, int displayNameLength, byte messageCommand, ByteBuffer data) {
      this.userId = userId;
      this.location = location;
      this.displayNameLength = displayNameLength;
      this.messageCommand = messageCommand;
      this.data = data;
    }

    @Override
    public void processInput(ChannelHandlerContext ctx) {
      byte[] displayNameBytes = new byte[displayNameLength];
      data.get(displayNameBytes);
      String displayName = new String(displayNameBytes, Config.MESSAGE_ENCODING);
      CentralEvents.fireReceiveMessage(userId, location, displayName, data.slice());
    }
  }

  public static class Out implements Outbound, SequentialMessage {

    public final int tag;
    public final String displayName;
    private final byte[] displayNameBytes;
    public final byte messageCommand;
    public final ByteBuffer messageBytes;

    public Out(int tag, String displayName, byte messageCommand, ByteBuffer messageBytes) {
      this.tag = tag;
      this.displayName = displayName;
      this.displayNameBytes = displayName.getBytes(Config.MESSAGE_ENCODING);
      if (displayNameBytes.length > 40) {
        throw new IllegalArgumentException("Display name must be up to 40 bytes");
      }
      this.messageCommand = messageCommand;
      this.messageBytes = messageBytes;
    }

    @Override
    public ChannelFuture processOutput(ChannelHandlerContext ctx, Session session) {
      byte[] sessionIdBytes = new byte[8];
      session.sessionId.readBytes(sessionIdBytes);
      final byte sequenceNo = session.sessionId.clientSequenceNo(); // copy
      int byteSize = byteSize();
      ByteBuf buf = ctx.alloc().buffer(byteSize, byteSize)
          .writeByte(Headers.mask(Headers.TEXT, Headers.COMPLETE))
          .writeBytes(sessionIdBytes)
          .writeByte(messageCommand)
          .writeByte((byte) (displayNameBytes.length))
          .writeBytes(displayNameBytes, 0, displayNameBytes.length)
          .writeBytes(messageBytes);
      ChannelFuture cf = ctx.writeAndFlush(new DatagramPacket(buf, session.address));
      cf.addListener(new ChannelFutureListener() {

        @Override
        public void operationComplete(ChannelFuture future) throws Exception {
          ByteBuffer copy = messageBytes.duplicate();
          copy.position(0);
          CentralEvents.fireSendMessage(tag, sequenceNo, displayName, copy);
        }
      });
      return cf;
    }

    @Override
    public int byteSize() {
      return 11 + displayNameBytes.length + messageBytes.remaining();
    }

    @Override
    public int tag() {
      return tag;
    }
  }
}
