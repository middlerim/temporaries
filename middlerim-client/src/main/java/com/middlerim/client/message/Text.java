package com.middlerim.client.message;

import com.middlerim.client.Config;
import com.middlerim.client.central.CentralEvents;
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
    public final ByteBuf data;

    public In(long userId, Coordinate location, int displayNameLength, byte messageCommand, ByteBuf data) {
      this.userId = userId;
      this.location = location;
      this.displayNameLength = displayNameLength;
      this.messageCommand = messageCommand;
      this.data = data;
    }

    @Override
    public void processInput(ChannelHandlerContext ctx) {
      try {
        byte[] displayNameBytes = new byte[displayNameLength];
        data.readBytes(displayNameBytes);
        String displayName = new String(displayNameBytes, Config.MESSAGE_ENCODING);
        CentralEvents.fireReceiveMessage(userId, location, displayName, data.slice());
      } finally {
        data.release();
      }
    }
  }

  public static class Out implements Outbound, SequentialMessage {

    public final int tag;
    public final String displayName;
    private final byte[] displayNameBytes;
    public final byte messageCommand;
    public final byte[] messageBytes;

    public Out(int tag, String displayName, byte messageCommand, byte[] messageBytes) {
      this.tag = tag;
      this.displayName = displayName;
      this.displayNameBytes = displayName.getBytes(Config.MESSAGE_ENCODING);
      if (displayNameBytes.length > Config.MAX_DISPLAY_NAME_BYTE_LENGTH) {
        throw new IllegalArgumentException("Display name must be up to " + Config.MAX_DISPLAY_NAME_BYTE_LENGTH + " bytes");
      }
      this.messageCommand = messageCommand;
      if (messageBytes.length > Config.MAX_TEXT_BYTES) {
        throw new IllegalArgumentException("Message must be up to " + Config.MAX_TEXT_BYTES + " bytes");
      }
      this.messageBytes = messageBytes;
    }

    @Override
    public ChannelFuture processOutput(ChannelHandlerContext ctx, Session session) {
      byte[] sessionIdBytes = new byte[8];
      session.sessionId.readBytes(sessionIdBytes);
      int byteSize = byteSize();
      ByteBuf buf = ctx.alloc().buffer(byteSize, byteSize)
          .writeByte(Headers.Command.TEXT.code)
          .writeBytes(sessionIdBytes)
          .writeInt(tag)
          .writeByte(messageCommand)
          .writeByte((byte) (displayNameBytes.length))
          .writeBytes(displayNameBytes, 0, displayNameBytes.length)
          .writeBytes(messageBytes);
      ChannelFuture cf = ctx.writeAndFlush(new DatagramPacket(buf, session.address));
      cf.addListener(new ChannelFutureListener() {

        @Override
        public void operationComplete(ChannelFuture future) throws Exception {
          CentralEvents.fireSendMessage(tag, displayName, messageBytes);
        }
      });
      return cf;
    }

    @Override
    public int byteSize() {
      return 15 + displayNameBytes.length + messageBytes.length;
    }

    @Override
    public int tag() {
      return tag;
    }
  }
}
