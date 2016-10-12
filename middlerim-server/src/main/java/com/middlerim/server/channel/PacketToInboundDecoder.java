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
    if (LOG.isDebugEnabled()) {
      LOG.debug("Received packet: " + msg);
    }
    byte header = in.readByte();
    if (header == Headers.ASSIGN_AID) {
      if (in.readableBytes() > 0) {
        // Invalid access.
        return;
      }
      Session anonymous = Sessions.getOrCreateSession(SessionId.ANONYMOUS, msg.sender());
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
        return;
      }
      Session session = Sessions.getSession(sessionId);
      if (session != null) {
        Sessions.remove(session);
      }
      return;
    }
    if (header == Headers.AGAIN) {
      if (in.readableBytes() > 0) {
        // Invalid access.
        return;
      }
      Session session = Sessions.getSession(sessionId);
      if (session == null) {
        // Ignore the request.
        return;
      }
      session.address = msg.sender();
      Outbound lastSentMessage = Messages.getMessage(session.sessionId.userId(), sessionId.sequenceNo());
      if (lastSentMessage == null) {
        ctx.channel().write(new OutboundMessage<>(session, Markers.NOTFOUND));
        return;
      }
      // Re-send the message.
      ctx.channel().write(new OutboundMessage<>(session, lastSentMessage));
    } else if (Headers.isMasked(header, Headers.RECEIVED)) {
      Messages.removeMessage(sessionId.userId(), sessionId.sequenceNo());
    }
    if (!Headers.hasData(header)) {
      return;
    }
    Session session = Sessions.getOrCreateSession(sessionId, msg.sender());
    ctx.channel().attr(AttributeKeys.SESSION).set(session);

    if (Headers.isMasked(header, Headers.LOCATION)) {
      int latitude = in.readInt();
      int longtitude = in.readInt();
      if (in.readableBytes() > 0) {
        // Invalid access.
        return;
      }
      Point point = new Point(latitude, longtitude);
      out.add(new Location(point));
      return;
    }
    if (!session.sessionId.validateAndRefresh(sessionId)) {
      // Must be sequential
      ctx.channel().write(new OutboundMessage<>(session, Markers.INVALID_SEQUENCE));
      return;
    }
    ctx.channel().writeAndFlush(new OutboundMessage<>(session, Markers.RECEIVED));

    if (Headers.isMasked(header, Headers.FRAGMENT)) {
      int paylaadSize = in.readInt();
      int offset = in.readInt();
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
        out.add(new Text(buff.asReadOnlyBuffer(), messageCommand));
      }
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
