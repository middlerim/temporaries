package com.middlerim.server.command.channel;

import java.nio.BufferUnderflowException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.middlerim.location.Point;
import com.middlerim.server.Headers;
import com.middlerim.server.Headers.Header;
import com.middlerim.server.channel.PacketToInboundDecoder;
import com.middlerim.server.command.Config;
import com.middlerim.server.command.message.Location;
import com.middlerim.server.command.message.Text;
import com.middlerim.server.command.storage.Messages;
import com.middlerim.server.message.Markers;
import com.middlerim.server.message.OutboundMessage;
import com.middlerim.session.Session;
import com.middlerim.session.SessionId;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;

public class CommandHandler extends PacketToInboundDecoder {
  private static final Logger LOG = LoggerFactory.getLogger(CommandHandler.class);

  @Override
  public boolean isSharable() {
    return true;
  }

  @Override
  protected void acceptReceived(SessionId clientSessionId) {
    Messages.removeMessage(clientSessionId.userId(), clientSessionId.serverSequenceNo());
  }

  @Override
  protected boolean handleNonSequentialRequest(ChannelHandlerContext ctx, Header header, Session session, SessionId clientSessionId, ByteBuf in, List<Object> out) {
    if (header == Headers.Command.LOCATION) {
      if (!session.sessionId.validateClient(clientSessionId)) {
        LOG.warn("Packet[LOCATION] - {}: Ignored(Invalid session ID on client={}).", session, clientSessionId);
        return true;
      }
      int latitude = in.readInt();
      int longtitude = in.readInt();
      if (in.readableBytes() > 0) {
        // Invalid access.
        LOG.warn("Packet[LOCATION] - {}: Ignored(Invalid length). {}", session, in);
        return true;
      }
      Point point = new Point(latitude, longtitude);
      LOG.debug("Packet[LOCATION] - {}: {}", session, point);
      out.add(new Location(session, point));
      return true;
    }
    return false;
  }

  @Override
  protected boolean handleSequentialRequest(ChannelHandlerContext ctx, Header header, int tag, Session session, SessionId clientSessionId, ByteBuf in, List<Object> out) {
    // Send RECEIVED response when receiving message which implements SequencialMessage respectively.

    ctx.channel().attr(AttributeKeys.SESSION).set(session);

    if (header == Headers.Command.TEXT) {
      byte messageCommand = in.readByte();
      byte displayNameLength = in.readByte();
      LOG.debug("Packet[TEXT] - {}: tag={}, buff={}", session, tag, in);
      out.add(new Text.In(tag, displayNameLength, messageCommand, in.retain()));
      return true;
    }
    return false;
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
    ctx.channel().writeAndFlush(new OutboundMessage<>(ctx.channel().attr(AttributeKeys.SESSION).get(), Markers.INVALID_DATA));
    // Ignore BufferUnderFlowException since length of the packet isn't validated deliberately in advance.
    if (Config.TEST | !(cause instanceof BufferUnderflowException || (cause.getCause() != null && cause.getCause() instanceof BufferUnderflowException))) {
      LOG.error("Unexpected exception", cause);
    }
  }

}
