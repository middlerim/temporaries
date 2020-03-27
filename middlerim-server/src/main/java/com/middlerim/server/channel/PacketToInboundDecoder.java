package com.middlerim.server.channel;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.middlerim.server.Headers;
import com.middlerim.server.Headers.Header;
import com.middlerim.server.message.Markers;
import com.middlerim.server.message.OutboundMessage;
import com.middlerim.server.storage.Sessions;
import com.middlerim.session.Session;
import com.middlerim.session.SessionId;
import com.middlerim.token.TokenIssuer;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.DatagramPacket;
import io.netty.handler.codec.MessageToMessageDecoder;

public abstract class PacketToInboundDecoder extends MessageToMessageDecoder<DatagramPacket> {
  private static final Logger LOG = LoggerFactory.getLogger(PacketToInboundDecoder.class);

  @Override
  protected void decode(ChannelHandlerContext ctx, DatagramPacket msg, List<Object> out) throws Exception {
    ByteBuf in = msg.content();
    if (!in.isReadable()) {
      return;
    }
    Header header = Headers.parse(in.readByte());
    if (header == Headers.Control.ASSIGN_AID) {
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

    if (header == Headers.Control.EXIT) {
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
    if (header == Headers.Control.ERROR) {
      Session session = Sessions.getValidatedSession(sessionId);
      if (session != null) {
        Sessions.remove(session);
      }
      LOG.error("Packet[ERROR]: Client is having unexpected error {}({})", session != null ? session : sessionId, msg.sender());
      return;
    }

    if (header == Headers.Control.RECEIVED) {
      acceptReceived(sessionId);
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

    // --- Handle non-sequential message.
    if (handleNonSequentialRequest(ctx, header, session, sessionId, in, out)) {
      return;
    }

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
    if (!handleSequentialRequest(ctx, header, tag, session, sessionId, in, out)) {
      LOG.warn("Unkown request received from {}. in: {}", session, in);
      Sessions.remove(session);
    }
  }

  protected abstract void acceptReceived(SessionId clientSessionId);

  protected abstract boolean handleNonSequentialRequest(ChannelHandlerContext ctx, Header header, Session session, SessionId clientSessionId, ByteBuf in, List<Object> out);
  protected abstract boolean handleSequentialRequest(ChannelHandlerContext ctx, Header header, int tag, Session session, SessionId clientSessionId, ByteBuf in, List<Object> out);
}
