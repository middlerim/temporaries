package com.middlerim.server.message;

import java.nio.ByteBuffer;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.middlerim.location.Point;
import com.middlerim.message.Inbound;
import com.middlerim.message.Outbound;
import com.middlerim.message.SequentialMessage;
import com.middlerim.server.Headers;
import com.middlerim.server.InvalidDataException;
import com.middlerim.server.MessageCommands;
import com.middlerim.server.channel.AttributeKeys;
import com.middlerim.server.storage.Locations;
import com.middlerim.server.storage.location.LocationStorage;
import com.middlerim.session.Session;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.DatagramPacket;

public class Text {
  private static final Logger LOG = LoggerFactory.getLogger(Text.class);

  public static class TextReceived implements Outbound {

    private static final int FIXED_BYTE_SIZE = 9;

    private final int tag;
    private final int numberOfDelivery;
    private TextReceived(int tag, int numberOfDelivery) {
      this.tag = tag;
      this.numberOfDelivery = numberOfDelivery;
    }

    @Override
    public ChannelFuture processOutput(ChannelHandlerContext ctx, Session recipient) {
      return ctx.writeAndFlush(new DatagramPacket(ctx.alloc().buffer(FIXED_BYTE_SIZE, FIXED_BYTE_SIZE)
          .writeByte(Headers.mask(Headers.RECEIVED, Headers.TEXT))
          .writeInt(tag)
          .writeInt(numberOfDelivery), recipient.address));
    }

    @Override
    public int byteSize() {
      return FIXED_BYTE_SIZE;
    }
  }

  public static class In implements Inbound {
    public final int tag;
    public final byte displayNameLength;
    public final byte messageCommand;
    public final ByteBuffer data;

    public In(int tag, byte displayNameLength, byte messageCommand, ByteBuffer data) {
      this.tag = tag;
      this.displayNameLength = displayNameLength;
      this.messageCommand = messageCommand;
      this.data = data;
    }

    private List<LocationStorage.Entry> findrecipients(Session session) {
      if (MessageCommands.isArea(messageCommand)) {
        return Locations.findAround(session.sessionId, messageCommand);
      } else {
        throw new InvalidDataException("Unkown message command is revceived");
      }
    }

    @Override
    public void processInput(ChannelHandlerContext ctx) {
      Session session = ctx.channel().attr(AttributeKeys.SESSION).get();
      Point point = Locations.findBySessionId(session.sessionId);
      if (point == null) {
        LOG.warn("Could not find a location for {}", session);
        ctx.channel().write(new OutboundMessage<>(session, new TextReceived(tag, 0)));
        return;
      }
      int dataLength = data.remaining();
      List<LocationStorage.Entry> recipients = findrecipients(session);
      ctx.channel().write(new OutboundMessage<>(session, new TextReceived(tag, recipients.size())));
      for (LocationStorage.Entry entry : recipients) {
        Session recipient = entry.session();
        ctx.channel().write(new OutboundMessage<>(recipient, new Out(tag, point, displayNameLength, messageCommand, data, dataLength)));
      }
    }
  }

  public static class Out implements Outbound, SequentialMessage {

    public final int tag;
    public final Point point;
    public final int displayNameLength;
    public final byte messageCommand;
    public final ByteBuffer data;
    public final int dataLength;

    public Out(int tag, Point point, byte displayNameLength, byte messageCommand, ByteBuffer data, int dataLength) {
      this.tag = tag;
      this.point = point;
      this.displayNameLength = displayNameLength;
      this.messageCommand = messageCommand;
      this.data = data;
      this.dataLength = dataLength;
    }

    @Override
    public ChannelFuture processOutput(ChannelHandlerContext ctx, Session recipient) {
      int byteSize = byteSize();
      byte[] userIdBytes = new byte[4];
      Session session = ctx.channel().attr(AttributeKeys.SESSION).get();
      session.sessionId.readUserIdBytes(userIdBytes);

      ByteBuf buf = ctx.alloc().buffer(byteSize, byteSize)
          .writeByte(Headers.mask(Headers.TEXT, Headers.COMPLETE))
          .writeShort(recipient.sessionId.serverSequenceNo())
          .writeBytes(userIdBytes)
          .writeInt(point.latitude)
          .writeInt(point.longitude)
          .writeByte(displayNameLength)
          .writeByte(messageCommand);
      synchronized (data) {
        // the data is shared with other threads.
        data.position(0);
        buf.writeBytes(data);
      }
      return ctx.writeAndFlush(new DatagramPacket(buf, recipient.address));
    }

    @Override
    public int byteSize() {
      return 17 + dataLength;
    }

    /**
     * Unique message ID by each message senders.
     */
    @Override
    public int tag() {
      return tag;
    }
  }
}
