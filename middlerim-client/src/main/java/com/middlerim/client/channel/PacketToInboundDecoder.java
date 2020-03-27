package com.middlerim.client.channel;

import java.nio.BufferUnderflowException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.middlerim.client.Config;
import com.middlerim.client.central.CentralEvents;
import com.middlerim.client.message.Markers;
import com.middlerim.client.message.Text;
import com.middlerim.client.session.Sessions;
import com.middlerim.client.view.ViewContext;
import com.middlerim.location.Point;
import com.middlerim.message.SequentialMessage;
import com.middlerim.server.Headers;
import com.middlerim.server.Headers.Header;
import com.middlerim.session.Session;
import com.middlerim.session.SessionId;
import com.middlerim.token.TokenIssuer;
import com.middlerim.util.Bytes;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.DatagramPacket;
import io.netty.handler.codec.MessageToMessageDecoder;

public class PacketToInboundDecoder extends MessageToMessageDecoder<DatagramPacket> {
  private static final Logger LOG = LoggerFactory.getLogger(Config.INTERNAL_APP_NAME);

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
    Header header = Headers.parse(in.readByte());
    if (header == Headers.Control.ASSIGN_AID) {
      Session session = Sessions.getSession();
      if (!session.isNotAssigned()) {
        // Already assigned. It happens because it's not synchronizing while ASSIGN_AID is being requested.
        LOG.info("Packet[ASSGIN_AID]: Ignored since annonymous ID has been assinged already.");
        return;
      }
      byte[] tokenBytes = new byte[8];
      in.readBytes(tokenBytes);
      SessionId sessionId = TokenIssuer.decodeTosessionId(tokenBytes);
      Sessions.setSession(Session.create(sessionId, Sessions.getSession().address));
      LOG.debug("Packet[ASSGIN_AID]: Received new anonymous session ID: {}", sessionId);
      CentralEvents.fireReceived(SequentialMessage.UNASSIGNED);
      return;
    }
    if (header == Headers.Control.UPDATE_AID) {
      byte[] userIdBytes = new byte[4];
      in.readBytes(userIdBytes);
      SessionId sessionId = Sessions.getSession().sessionId.copyWithNewUserId(userIdBytes);
      Sessions.setSession(Session.create(sessionId, Sessions.getSession().address));
      LOG.debug("Packet[UPDATE_AID]: Received new anonymous user ID: {}", sessionId);
      return;
    }

    Session session = Sessions.getSession();
    if (session == null || !session.address.equals(msg.sender())) {
      // sender address must be same to prevent malicious packet.
      LOG.warn("Access from unknown address: {}, current session is {}", msg.sender(), session);
      return;
    }

    if (header == Headers.Control.AGAIN) {
      // Synchronize sequenceNo on the client with central server.
      byte clientSequenceNo = in.readByte();
      int tag = in.readInt();
      LOG.debug("Packet[AGAIN]: tag={}, clientSequenceNo on central server={}, on the client={}", tag, session.sessionId.clientSequenceNo());
      session.sessionId.synchronizeClientWithServer(clientSequenceNo);
      out.add(new Markers.Again(tag));
      return;
    }

    if (header == Headers.Control.RECEIVED) {
      int tag = in.readInt();
      LOG.debug("Packet[RECEIVED]: tag={}", tag);
      CentralEvents.fireReceived(tag);
      return;
    }
    if (header == Headers.Command.TEXT_RECEIVED) {
      int tag = in.readInt();
      int numberOfDelivery = in.readInt();
      LOG.debug("Packet[RECEIVED&TEXT]: {}", numberOfDelivery);
      CentralEvents.fireReceived(tag);
      CentralEvents.fireReceivedText(tag, numberOfDelivery);
      return;
    }

    short serverSequenceNo = in.readShort();
    if (!session.sessionId.validateServerSequenceNoAndRefresh(serverSequenceNo)) {
      // This isn't an error. Messages from server can be asynchronous.
      LOG.debug("The incoming message is not sequential. on client={} != on server={}. ", (short) (session.sessionId.serverSequenceNo() + 1), serverSequenceNo);
    }

    ctx.channel().writeAndFlush(new Markers.Received(serverSequenceNo));

    if (header == Headers.Command.TEXT) {
      byte[] userIdBytes = new byte[4];
      in.readBytes(userIdBytes);
      int latitude = in.readInt();
      int longtitude = in.readInt();
      byte displayNameLength = in.readByte();
      byte messageCommand = in.readByte();
      if (in.readableBytes() <= displayNameLength) {
        LOG.warn("Received insufficient data. data length need at least {} but actually it's {}", displayNameLength, in.readableBytes());
        return;
      }
      LOG.debug("Packet[TEXT]: {}", in);
      out.add(new Text.In(Bytes.intToLong(userIdBytes), new Point(latitude, longtitude).toCoordinate(), displayNameLength, messageCommand, in.retain()));
      return;
    }
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
