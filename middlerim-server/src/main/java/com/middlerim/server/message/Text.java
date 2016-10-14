package com.middlerim.server.message;

import java.nio.ByteBuffer;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.DatagramPacket;

public class Text implements Inbound, Outbound, SequentialMessage {
  private static final Logger logger = LoggerFactory.getLogger(Text.class);

  public final ByteBuffer messageBytes;
  public final byte messageCommand;

  public Text(ByteBuffer messageBytes, byte messageCommand) {
    this.messageBytes = messageBytes;
    this.messageCommand = messageCommand;
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
    List<LocationStorage.Entry> recipients = findrecipients(session);
    for (LocationStorage.Entry entry : recipients) {
      Session recipient = entry.session();
      ctx.channel().write(new OutboundMessage<>(recipient, this));
    }
    if (logger.isDebugEnabled()) {
      logger.debug("Received text: " + session + "(buffer capacity: " + messageBytes.capacity() + ")");
    }
  }

  @Override
  public ChannelFuture processOutput(ChannelHandlerContext ctx, Session session) {
    int byteSize = byteSize();
    if (logger.isDebugEnabled()) {
      logger.debug("Sent text: " + session);
    }
    return ctx.writeAndFlush(new DatagramPacket(
        ctx.alloc().buffer(byteSize, byteSize)
            .writeByte(Headers.mask(Headers.TEXT, Headers.COMPLETE))
            .writeShort(session.sessionId.sequenceNo())
            .writeByte(messageCommand)
            .writeBytes(messageBytes),
        session.address));
  }

  @Override
  public int byteSize() {
    return 4 + messageBytes.remaining();
  }
}
