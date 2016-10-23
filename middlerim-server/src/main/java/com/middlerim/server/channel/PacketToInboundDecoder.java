package com.middlerim.server.channel;

import java.io.RandomAccessFile;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.middlerim.location.Point;
import com.middlerim.message.Outbound;
import com.middlerim.server.Config;
import com.middlerim.server.Headers;
import com.middlerim.server.message.Location;
import com.middlerim.server.message.Markers;
import com.middlerim.server.message.OutboundMessage;
import com.middlerim.server.message.Text;
import com.middlerim.server.storage.Messages;
import com.middlerim.server.storage.Sessions;
import com.middlerim.session.Session;
import com.middlerim.session.SessionId;
import com.middlerim.session.SessionListener;
import com.middlerim.token.TokenIssuer;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.DatagramPacket;
import io.netty.handler.codec.MessageToMessageDecoder;

public class PacketToInboundDecoder extends MessageToMessageDecoder<DatagramPacket> {
  private static final Logger LOG = LoggerFactory.getLogger(PacketToInboundDecoder.class);
  private static Map<Session, ByteBuffer> fragmentedBufferMap = new ConcurrentHashMap<>();

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
      Session anonymous = Sessions.getOrCreateSession(SessionId.ANONYMOUS, msg.sender());
      LOG.debug("Packet[ASSIGN_AID]: {}", anonymous);
      ctx.channel().write(new OutboundMessage<>(anonymous, Markers.ASSIGN_AID));
      return;
    }

    byte[] tokenBytes = new byte[8];
    in.readBytes(tokenBytes);
    // TODO Validate the token and convert to session ID
    SessionId sessionId = TokenIssuer.decodeTosessionId(tokenBytes);

    if (header == Headers.EXIT) {
      if (in.readableBytes() > 0) {
        // Invalid access.
        LOG.warn("Packet[EXIT]: Ignored(Invalid length). {}", in);
        return;
      }
      Session session = Sessions.getSession(sessionId);
      LOG.debug("Packet[EXIT]: {}", sessionId);
      if (session != null) {
        Sessions.remove(session);
      } else {
        LOG.warn("Packet[EXIT]: Ignored(Exited already). {}", sessionId);
      }
      return;
    }
    if (header == Headers.ERROR) {
      Session session = Sessions.getSession(sessionId);
      if (session != null) {
        Sessions.remove(session);
      }
      LOG.error("Packet[ERROR]: Client is having unexpected error {}({})", session != null ? session : sessionId, msg.sender());
      return;
    }
    if (header == Headers.AGAIN) {
      if (in.readableBytes() > 0) {
        // Invalid access.
        LOG.warn("Packet[AGAIN]: Ignored(Invalid length). {}, {}", sessionId, in);
        return;
      }
      Session session = Sessions.getSession(sessionId);
      if (session == null) {
        // Ignore the request.
        LOG.warn("Packet[AGAIN]: Ignored(The session couldn't be found). {}", sessionId);
        return;
      }
      session.address = msg.sender();
      Outbound lastSentMessage = Messages.getMessage(session.sessionId.userId(), sessionId.serverSequenceNo());
      if (lastSentMessage == null) {
        LOG.warn("Packet[AGAIN]: Ignored(Message not found). {}", sessionId);
        ctx.channel().write(new OutboundMessage<>(session, Markers.NOTFOUND));
        return;
      }
      // Re-send the message.
      LOG.debug("Packet[AGAIN]: {}", sessionId);
      ctx.channel().write(new OutboundMessage<>(session, lastSentMessage));
    } else if (Headers.isMasked(header, Headers.RECEIVED)) {
      Messages.removeMessage(sessionId.userId(), sessionId.serverSequenceNo());
    }
    if (!Headers.hasData(header)) {
      return;
    }
    Session session = Sessions.getOrCreateSession(sessionId, msg.sender());
    LOG.debug("Session for the packet: {}", session);
    ctx.channel().attr(AttributeKeys.SESSION).set(session);

    if (Headers.isMasked(header, Headers.LOCATION)) {
      int latitude = in.readInt();
      int longtitude = in.readInt();
      if (in.readableBytes() > 0) {
        // Invalid access.
        LOG.warn("Packet[LOCATION] - {}: Ignored(Invalid length). {}", session, in);
        return;
      }
      Point point = new Point(latitude, longtitude);
      LOG.debug("Packet[LOCATION] - {}: {}", session, point);
      out.add(new Location(point));
      return;
    }
    if (!session.sessionId.validateClientAndRefresh(sessionId)) {
      // Must be sequential
      if (session.sessionId.clientSequenceNo() == sessionId.clientSequenceNo()) {
        // Already received but the client hasn't received RECEIVED message yet.
        LOG.debug("Invalid sequenceNo - {}: Discard the packet, already received.", session);
        ctx.channel().write(new OutboundMessage<>(session, Markers.RECEIVED));
        return;
      } else {
        if (LOG.isDebugEnabled()) {
          LOG.debug("Invalid sequenceNo - {}: {} != {}", session, (byte) (session.sessionId.clientSequenceNo() + 1), sessionId.clientSequenceNo());
        }
        ctx.channel().write(new OutboundMessage<>(session, Markers.INVALID_SEQUENCE));
        return;
      }
    }

    if (Headers.isMasked(header, Headers.FRAGMENT)) {
      int paylaadSize = in.readInt();
      int offset = in.readInt();
      LOG.debug("Packet[FRAGMENT] - {}: {}/{}", session, offset, paylaadSize);
      consumePayload(session, paylaadSize, offset, in);
    }
    if (!Headers.isMasked(header, Headers.COMPLETE)) {
      // Wait for completion of consuming all payload.
    } else {
      ByteBuffer buff = fragmentedBufferMap.remove(session);
      if (buff == null) {
        buff = in.nioBuffer(in.readerIndex(), in.readableBytes());
      }
      if (Headers.isMasked(header, Headers.TEXT)) {
        byte messageCommand = buff.get();
        byte displayNameLength = buff.get();
        LOG.debug("Packet[TEXT] - {}: {}", session, buff);
        out.add(new Text.In(displayNameLength, messageCommand, buff.slice()));
      }
    }

    // Send RECEIVED response when receiving message which implements SequencialMessage except TEXT.
    // When receiving TEXT message, TextReceived response is going to be sent instead.
    if (!Headers.isMasked(header, Headers.TEXT)) {
      ctx.channel().writeAndFlush(new OutboundMessage<>(session, Markers.RECEIVED));
    }
  }

  private void consumePayload(Session session, int paylaadSize, int offset, ByteBuf in) throws Exception {
    ByteBuffer cached = fragmentedBufferMap.get(session);
    if (cached == null) {
      if (paylaadSize > 10000000) {
        cached = new RandomAccessFile(session.sessionId + ".dat", "rw").getChannel().map(FileChannel.MapMode.READ_WRITE, 0, paylaadSize);
      } else {
        cached = MappedByteBuffer.allocate(paylaadSize);
      }
      fragmentedBufferMap.put(session, cached);
    }
    cached.position(offset);
    in.getBytes(0, cached);
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
        fragmentedBufferMap.remove(session);
      }
    });
  }
}
