package com.middlerim.server.command.channel;

import java.nio.BufferUnderflowException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.middlerim.location.Point;
import com.middlerim.server.Headers;
import com.middlerim.server.command.Config;
import com.middlerim.server.command.message.Location;
import com.middlerim.server.command.message.Markers;
import com.middlerim.server.command.message.OutboundMessage;
import com.middlerim.server.command.message.Text;
import com.middlerim.server.command.storage.Messages;
import com.middlerim.server.command.storage.SessionListener;
import com.middlerim.server.command.storage.Sessions;
import com.middlerim.session.Session;
import com.middlerim.session.SessionId;
import com.middlerim.token.TokenIssuer;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.DatagramPacket;
import io.netty.handler.codec.MessageToMessageDecoder;

public class PacketToInboundDecoder extends MessageToMessageDecoder<DatagramPacket> {
  private static final Logger LOG = LoggerFactory.getLogger(PacketToInboundDecoder.class);

  @Override
  protected void decode(ChannelHandlerContext ctx, DatagramPacket msg, List<Object> out) throws Exception {
    ByteBuf in = msg.content();
    if (!in.isReadable()) {
      return;
    }
    byte header = in.readByte();
    if (header == Headers.ASSIGN_AID) {
      if (in.readableBytes() > 0) {
        // Invalid access.
        LOG.warn("Packet[ASSIGN_AID]: Ignored(Invalid length). {}", in);
        return;
      }
      Session anonymous = Sessions.getOrCreateAnonymous(msg.sender());
      LOG.debug("Packet[ASSIGN_AID]: {}", anonymous);
      ctx.channel().write(new OutboundMessage<>(anonymous, Markers.ASSIGN_AID));
      return;
    }

    byte[] tokenBytes = new byte[8];
    in.readBytes(tokenBytes);
    SessionId sessionId = TokenIssuer.decodeTosessionId(tokenBytes);

    if (header == Headers.EXIT) {
      if (in.readableBytes() > 0) {
        // Invalid access.
        LOG.warn("Packet[EXIT]: Ignored(Invalid length). {}", in);
        return;
      }
      Session session = Sessions.getValidatedSession(sessionId);
      LOG.debug("Packet[EXIT]: {}", sessionId);
      if (session != null) {
        Sessions.remove(session);
      } else {
        LOG.warn("Packet[EXIT]: Ignored(Exited already). {}", sessionId);
      }
      return;
    }
    if (header == Headers.ERROR) {
      Session session = Sessions.getValidatedSession(sessionId);
      if (session != null) {
        Sessions.remove(session);
      }
      LOG.error("Packet[ERROR]: Client is having unexpected error {}({})", session != null ? session : sessionId, msg.sender());
      return;
    }

    if (Headers.isMasked(header, Headers.RECEIVED)) {
      Messages.removeMessage(sessionId.userId(), sessionId.serverSequenceNo());
    }

    if (!Headers.hasData(header)) {
      return;
    }

    Session session = Sessions.getOrCreateSession(sessionId, msg.sender());
    if (session == null) {
      LOG.warn("The packet is discarded: {}", msg);
      return;
    }
    LOG.debug("Session for the packet: {}", session);
    if (!session.sessionId.equals(sessionId)) {
      session.sessionId.synchronizeClientWithServer((byte) (sessionId.clientSequenceNo() - 1));
      LOG.debug("Session is removed. Assigned new session: {}", session);
      ctx.channel().write(new OutboundMessage<>(session, Markers.UPDATE_AID));
      return;
    }

    ctx.channel().attr(AttributeKeys.SESSION).set(session);

    // --- Handle non-sequential message.

    if (Headers.isMasked(header, Headers.LOCATION)) {
      if (!session.sessionId.validateClient(sessionId)) {
        LOG.warn("Packet[LOCATION] - {}: Ignored(Invalid session ID on client={}).", session, sessionId);
        return;
      }
      int latitude = in.readInt();
      int longtitude = in.readInt();
      if (in.readableBytes() > 0) {
        // Invalid access.
        LOG.warn("Packet[LOCATION] - {}: Ignored(Invalid length). {}", session, in);
        return;
      }
      Point point = new Point(latitude, longtitude);
      LOG.debug("Packet[LOCATION] - {}: {}", session, point);
      out.add(new Location(session.sessionId, point));
      return;
    }

    // --- Handle Sequential message.

    int tag = in.readInt();
    if (!session.sessionId.validateClientAndRefresh(sessionId)) {
      // Must be sequential.
      if (session.sessionId.clientSequenceNo() == sessionId.clientSequenceNo()) {
        // Already received but the client hasn't received RECEIVED message yet.
        LOG.debug("Invalid sequenceNo - {}: Discard the packet, already received.", session);
        ctx.channel().write(new OutboundMessage<>(session, new Markers.Received(tag)));
        return;
      } else {
        if (LOG.isDebugEnabled()) {
          LOG.debug("Invalid sequenceNo - {}: {} != {}", session, (byte) (session.sessionId.clientSequenceNo() + 1), sessionId.clientSequenceNo());
        }
        ctx.channel().write(new OutboundMessage<>(session, new Markers.InvalidSequence(tag)));
        return;
      }
    }

    if (Headers.isMasked(header, Headers.TEXT)) {
      byte messageCommand = in.readByte();
      byte displayNameLength = in.readByte();
      LOG.debug("Packet[TEXT] - {}: tag={}, buff={}", session, tag, in);
      out.add(new Text.In(tag, displayNameLength, messageCommand, in.nioBuffer()));
    }

    // Send RECEIVED response when receiving message which implements SequencialMessage except TEXT.
    // When receiving TEXT message, TextReceived response is going to be sent instead.
    if (!Headers.isMasked(header, Headers.TEXT)) {
      ctx.channel().writeAndFlush(new OutboundMessage<>(session, new Markers.Received(tag)));
    }
  }

  @Override
  public boolean isSharable() {
    return true;
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
    ctx.channel().writeAndFlush(new OutboundMessage<>(ctx.channel().attr(AttributeKeys.SESSION).get(), Markers.INVALID_DATA));
    // Ignore BufferUnderFlowException since length of the packet isn't validated deliberately in advance.
    if (Config.TEST | !(cause instanceof BufferUnderflowException || (cause.getCause() != null && cause.getCause() instanceof BufferUnderflowException))) {
      LOG.error("Unexpected exception", cause);
    }
  }

  static {
    Sessions.addListener(new SessionListener() {
      @Override
      public void onRemove(Session session) {
      }

      @Override
      public void onExpire(Session oldSession, Session newSession) {
        onRemove(oldSession);
      }
    });
  }
}
