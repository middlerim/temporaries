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
import com.middlerim.client.message.Markers;
import com.middlerim.client.message.MessageLost;
import com.middlerim.client.message.Text;
import com.middlerim.client.session.Sessions;
import com.middlerim.client.view.ViewContext;
import com.middlerim.location.Point;
import com.middlerim.server.Headers;
import com.middlerim.session.Session;
import com.middlerim.session.SessionId;
import com.middlerim.token.TokenIssuer;
import com.middlerim.util.Bytes;

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
        if (viewContext.isDebug()) {
          viewContext.logger().debug(NAME, "Packet[ASSGIN_AID]: Ignored since annonymous ID has been assinged already.");
        }
        return;
      }
      byte[] tokenBytes = new byte[8];
      in.readBytes(tokenBytes);
      SessionId sessionId = TokenIssuer.decodeTosessionId(tokenBytes);
      Sessions.setSession(Session.create(sessionId, Sessions.getSession().address));
      if (viewContext.isDebug()) {
        viewContext.logger().debug(NAME, "Packet[ASSGIN_AID]: Received new anonymous session ID." + sessionId);
      }
      CentralEvents.fireReceived(SessionId.UNASSIGNED_SEQUENCE_NO);
      return;
    }

    Session session = Sessions.getSession();
    if (session == null || !session.address.equals(msg.sender())) {
      // sender address must be same to prevent malicious packet.
      viewContext.logger().warn(NAME, "Access from unknown address: " + msg.sender() + ", current session is " + session);
      return;
    }

    if (header == Headers.AGAIN) {
      // Synchronize sequenceNo on the client with central server.
      byte clientSequenceNo = in.readByte();
      if (viewContext.isDebug()) {
        viewContext.logger().debug(NAME, "Packet[AGAIN]:  clientSequenceNo on central server=" + clientSequenceNo + ", on the client=" + session.sessionId.clientSequenceNo());
      }
      session.sessionId.synchronizeClientWithServer(clientSequenceNo);
      out.add(Markers.AGAIN);
      return;
    }

    if (Headers.isMasked(header, Headers.RECEIVED)) {
      byte clientSequenceNo = in.readByte();
      if (viewContext.isDebug()) {
        viewContext.logger().debug(NAME, "Packet[RECEIVED]: " + clientSequenceNo);
      }
      CentralEvents.fireReceived(clientSequenceNo);
      if (Headers.isMasked(header, Headers.TEXT)) {
        int numberOfDelivery = in.readInt();
        if (viewContext.isDebug()) {
          viewContext.logger().debug(NAME, "Packet[RECEIVED&TEXT]: " + numberOfDelivery);
        }
        CentralEvents.fireReceivedText(clientSequenceNo, numberOfDelivery);
      }
      return;
    }
    short serverSequenceNo = in.readShort();
    if (!session.sessionId.validateServerSequenceNoAndRefresh(serverSequenceNo)) {
      // Messages from server can be asynchronous. However, some messages are lost and will be requested.
      if (viewContext.isDebug()) {
        viewContext.logger().warn(NAME, "Invalid sequenceNo: " + (short) (session.sessionId.serverSequenceNo() + 1) + " != " + serverSequenceNo);
      }
      ctx.channel().write(new MessageLost(session.sessionId.serverSequenceNo(), serverSequenceNo));
    }
    ctx.channel().writeAndFlush(Markers.RECEIVED);

    if (Headers.isMasked(header, Headers.FRAGMENT)) {
      int paylaadSize = in.readInt();
      int offset = in.readInt();
      if (viewContext.isDebug()) {
        viewContext.logger().debug(NAME, "Packet[FRAGMENT]: " + offset + "/" + paylaadSize);
      }
      consumePayload(session, paylaadSize, offset, in);
      return;
    }
    if (!Headers.isMasked(header, Headers.COMPLETE)) {
      // Wait for completion of consuming all payload.
    } else {
      ByteBuffer buff = fragmentedBufferMap.remove(session);
      if (buff == null) {
        buff = in.nioBuffer(in.readerIndex(), in.readableBytes());
      }
      if (Headers.isMasked(header, Headers.TEXT)) {
        byte[] userIdBytes = new byte[4];
        buff.get(userIdBytes);
        int latitude = buff.getInt();
        int longtitude = buff.getInt();
        byte displayNameLength = buff.get();
        byte messageCommand = buff.get();
        if (buff.remaining() <= displayNameLength) {
          viewContext.logger().warn(NAME, "Received insufficient data. data length need at least " + displayNameLength + " but actually it's " + buff.remaining());
          return;
        }
        if (viewContext.isDebug()) {
          viewContext.logger().debug(NAME, "Packet[TEXT]: " + buff);
        }
        out.add(new Text.In(Bytes.intToLong(userIdBytes), new Point(latitude, longtitude).toCoordinate(), displayNameLength, messageCommand, buff.slice()));
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
    ctx.channel().writeAndFlush(Markers.INVALID_DATA);
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
