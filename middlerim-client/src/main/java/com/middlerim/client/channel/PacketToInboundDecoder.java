package com.middlerim.client.channel;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.middlerim.client.CentralEvents;
import com.middlerim.client.CentralServer;
import com.middlerim.client.Config;
import com.middlerim.client.message.Markers;
import com.middlerim.client.message.Messages;
import com.middlerim.client.message.OutboundMessage;
import com.middlerim.client.message.Text;
import com.middlerim.client.session.Sessions;
import com.middlerim.client.view.ViewContext;
import com.middlerim.server.Headers;
import com.middlerim.session.Session;
import com.middlerim.session.SessionId;
import com.middlerim.token.TokenIssuer;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.DatagramPacket;
import io.netty.handler.codec.MessageToMessageDecoder;

public class PacketToInboundDecoder extends MessageToMessageDecoder<DatagramPacket> {
  private static final String NAME = "CHANNEL";
  private static Map<Session, ByteBuffer> fragmentedBufferMap = new ConcurrentHashMap<>();
  private final ViewContext viewContext;

  public PacketToInboundDecoder(ViewContext viewContext) {
    super();
    this.viewContext = viewContext;
  }

  @Override
  protected void decode(ChannelHandlerContext ctx, DatagramPacket msg, List<Object> out) throws Exception {
    ByteBuf in = msg.content();
    if (!in.isReadable()) {
      return;
    }
    byte header = in.readByte();
    if (header == Headers.ASSIGN_AID) {
      if (!Sessions.getSession().isNotAssigned()) {
        // Already assigned. It happens because it's not synchronizing while ASSIGN_AID is being requested.
        return;
      }
      byte[] tokenBytes = new byte[8];
      in.readBytes(tokenBytes);
      SessionId sessionId = TokenIssuer.decodeTosessionId(tokenBytes);
      Sessions.setSession(Session.create(sessionId, CentralServer.serverAddress));
      viewContext.logger().debug(NAME, "Received new anonymous session ID.");
      CentralEvents.fireReceived(SessionId.UNASSIGNED_SEQUENCE_NO);
      return;
    }
    Session session = Sessions.getSession();
    short sequenceNo = in.readShort();

    viewContext.logger().debug(NAME, "New packet: " + session + ":" + sequenceNo);

    if (header == Headers.AGAIN) {
      OutboundMessage<?> lastSentMessage = Messages.getMessage(sequenceNo);
      if (lastSentMessage == null) {
        viewContext.logger().warn(NAME, "Was going to resent a message but the message is not found.");
        CentralEvents.fireLostMessage(sequenceNo);
        return;
      }
      if (session.sessionId.sequenceNo() != sequenceNo) {
        viewContext.logger().warn(NAME, "Was going to resent a message but the sessionId.sequenceNo is invalid status.");
        CentralEvents.fireLostMessage(sequenceNo);
        return;
      }
      if (session.sessionId.retry() < Config.MAX_RETRY) {
        viewContext.logger().warn(NAME, "Was going to resent a message but it has been retried " + Config.MAX_RETRY + " times.");
        CentralEvents.fireLostMessage(sequenceNo);
        return;
      }
      if (!lastSentMessage.recipient.address.getHostString().equals(msg.sender().getHostString())) {
        viewContext.logger().warn(NAME, "Was requested re-seinding message request but denied it since it's from different server."
            + lastSentMessage.recipient.address.getHostString() + ", " + msg.sender().getHostString());
        CentralEvents.fireLostMessage(sequenceNo);
        return;
      }
      // Re-send the message.
      ctx.channel().writeAndFlush(lastSentMessage);
      return;
    }
    // sender address must be reset AFTER AGAIN logic.
    session.address = msg.sender();

    if (header == Headers.RECEIVED) {
      CentralEvents.fireReceived(sequenceNo);
      return;
    }
    if (!session.sessionId.validateSequenceNoAndRefresh(sequenceNo)) {
      // SequenceNo must be sequential.
      ctx.channel().write(new OutboundMessage<>(session, Markers.INVALID_SEQUENCE));
      return;
    }
    ctx.channel().writeAndFlush(new OutboundMessage<>(Sessions.getSession(), Markers.RECEIVED));

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
        out.add(new Text(buff, messageCommand));
      }
    }
  }

  private void consumePayload(Session session, int payloadSize, int offset, ByteBuf in) throws Exception {
    ByteBuffer cached = fragmentedBufferMap.get(session);
    if (cached == null) {
      if (payloadSize > 10000000) {
        cached = new RandomAccessFile(
            new File(viewContext.getCacheDir(), "mddilerim-" + session.sessionId + ".dat"), "rw")
                .getChannel().map(FileChannel.MapMode.READ_WRITE, 0, payloadSize);
      } else {
        cached = MappedByteBuffer.allocate(payloadSize);
      }
      fragmentedBufferMap.put(session, cached);
    }
    cached.position(offset);
    in.getBytes(0, cached);
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
    ctx.channel().writeAndFlush(new OutboundMessage<>(Sessions.getSession(), Markers.INVALID_DATA));
    // Ignore BufferUnderFlowException since length of the packet isn't validated deliberately in advance.
    if (viewContext.isDebug() || !(cause instanceof BufferUnderflowException || (cause.getCause() != null && cause.getCause() instanceof BufferUnderflowException))) {
      cause.printStackTrace();
    }
  }

  @Override
  public boolean isSharable() {
    return true;
  }
}
