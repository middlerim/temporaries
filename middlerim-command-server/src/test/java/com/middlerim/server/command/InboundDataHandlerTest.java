package com.middlerim.server.command;

import static org.junit.Assert.*;

import java.net.InetSocketAddress;

import org.junit.Before;
import org.junit.Test;

import com.middlerim.location.Point;
import com.middlerim.server.Headers;
import com.middlerim.server.MessageCommands;
import com.middlerim.server.channel.InboundHandler;
import com.middlerim.server.channel.OutboundHandler;
import com.middlerim.server.command.Config;
import com.middlerim.server.command.channel.CommandHandler;
import com.middlerim.server.command.storage.Locations;
import com.middlerim.server.storage.Sessions;
import com.middlerim.session.SessionId;
import com.middlerim.token.TokenIssuer;
import com.middlerim.util.Bytes;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.unix.DatagramSocketAddress;

public class InboundDataHandlerTest {

  private EmbeddedChannel createChannel() {
    EmbeddedChannel channel = new EmbeddedChannel(new TestInboudDecoder(), new CommandHandler(), new InboundHandler(), new OutboundHandler());
    return channel;
  }

  private class TestInboudDecoder extends ChannelInboundHandlerAdapter {
    private InetSocketAddress recipient = DatagramSocketAddress.createUnresolved("localhost", 44445);
    private InetSocketAddress sender = DatagramSocketAddress.createUnresolved("localhost", 44444);
    private TestInboudDecoder() {
    }
    private TestInboudDecoder(InetSocketAddress recipient, InetSocketAddress sender) {
      this.recipient = recipient;
      this.sender = sender;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
      super.channelRead(ctx, new DatagramPacket((ByteBuf) msg, recipient, sender));
    }
  }

  @Before
  public void beforeTest() {
    Locations.clear();
    Sessions.clear();
  }

  private SessionId createNewSession() {
    EmbeddedChannel channel = createChannel();
    byte[] data = new byte[]{Headers.Control.ASSIGN_AID.code};
    channel.writeInbound(Unpooled.wrappedBuffer(data));
    DatagramPacket result = channel.readOutbound();
    byte[] tokenBytes = new byte[8];
    result.content().readByte();
    result.content().readBytes(tokenBytes);
    channel.close();
    SessionId sessionId = TokenIssuer.decodeTosessionId(tokenBytes);
    return sessionId;
  }

  @Test
  public void testException_invalidData() {
    EmbeddedChannel channel = createChannel();
    SessionId sessionId1 = createNewSession();
    byte[] sessionIdBytes1 = new byte[8];
    sessionId1.readBytes(sessionIdBytes1);

    
    byte[] data = new byte[]{Headers.Command.LOCATION.code, sessionIdBytes1[0], sessionIdBytes1[1], sessionIdBytes1[2], sessionIdBytes1[3], sessionIdBytes1[4], sessionIdBytes1[5], sessionIdBytes1[6], sessionIdBytes1[7],
        1, 2, 3, 4, 1, 2, 3, 4, 5};
    channel.writeInbound(Unpooled.wrappedBuffer(data));
    DatagramPacket result = channel.readOutbound();
    assertNull(result);
  }

  @Test
  public void testReceived() {
    EmbeddedChannel channel = createChannel();
    byte[] data = new byte[]{Headers.Control.RECEIVED.code, 0, 0, 0, 1, 0, 0, 7, 8};
    channel.writeInbound(Unpooled.wrappedBuffer(data));
    Object result = channel.readOutbound();
    assertNull(result);
  }

  @Test
  public void testLocation_2users() {
    SessionId sessionId1 = createNewSession();
    byte[] sessionIdBytes1 = new byte[8];
    sessionId1.readBytes(sessionIdBytes1);

    SessionId sessionId2 = createNewSession();
    byte[] sessionIdBytes2 = new byte[8];
    sessionId2.readBytes(sessionIdBytes2);

    {
      EmbeddedChannel channel = createChannel();
      byte[] user1 = new byte[]{Headers.Command.LOCATION.code, sessionIdBytes1[0], sessionIdBytes1[1], sessionIdBytes1[2], sessionIdBytes1[3], sessionIdBytes1[4], sessionIdBytes1[5], sessionIdBytes1[6], sessionIdBytes1[7],
          1, 2, 3, 4, 1, 2, 3, 4};
      channel.writeInbound(Unpooled.wrappedBuffer(user1));
    }
    {
      EmbeddedChannel channel = createChannel();
      byte[] user2 = new byte[]{Headers.Command.LOCATION.code, sessionIdBytes2[0], sessionIdBytes2[1], sessionIdBytes2[2], sessionIdBytes2[3], sessionIdBytes2[4], sessionIdBytes2[5], sessionIdBytes2[6], sessionIdBytes2[7],
          2, 2, 2, 2, 4, 4, 4, 4};
      channel.writeInbound(Unpooled.wrappedBuffer(user2));
    }
    {
      // User 1
      Point expectedUserLocation = new Point(Bytes.bytesToInt(new byte[]{1, 2, 3, 4}), Bytes.bytesToInt(new byte[]{1, 2, 3, 4}));
      assertEquals(expectedUserLocation, Locations.findBySessionId(sessionId1));
    }
    {
      // User 2
      Point expectedUserLocation = new Point(Bytes.bytesToInt(new byte[]{2, 2, 2, 2}), Bytes.bytesToInt(new byte[]{4, 4, 4, 4}));
      assertEquals(expectedUserLocation, Locations.findBySessionId(sessionId2));
    }

    assertEquals(2, Locations.size());
  }

  @Test
  public void testLocation_2users_sameLocation() {
    SessionId sessionId1 = createNewSession();
    byte[] sessionIdBytes1 = new byte[8];
    sessionId1.readBytes(sessionIdBytes1);

    SessionId sessionId2 = createNewSession();
    byte[] sessionIdBytes2 = new byte[8];
    sessionId2.readBytes(sessionIdBytes2);

    {
      EmbeddedChannel channel = createChannel();
      byte[] user1 = new byte[]{Headers.Command.LOCATION.code, sessionIdBytes1[0], sessionIdBytes1[1], sessionIdBytes1[2], sessionIdBytes1[3], sessionIdBytes1[4], sessionIdBytes1[5], sessionIdBytes1[6], sessionIdBytes1[7],
          1, 2, 3, 4, 1, 2, 3, 4};
      channel.writeInbound(Unpooled.wrappedBuffer(user1));
    }
    {
      EmbeddedChannel channel = createChannel();
      byte[] user2 = new byte[]{Headers.Command.LOCATION.code, sessionIdBytes2[0], sessionIdBytes2[1], sessionIdBytes2[2], sessionIdBytes2[3], sessionIdBytes2[4], sessionIdBytes2[5], sessionIdBytes2[6], sessionIdBytes2[7],
          0, 0, 1, 0, 0, 0, 0, 1};
      channel.writeInbound(Unpooled.wrappedBuffer(user2));
    }
    {
      // User 1
      Point expectedUserLocation = new Point(Bytes.bytesToInt(new byte[]{1, 2, 3, 4}), Bytes.bytesToInt(new byte[]{1, 2, 3, 4}));
      assertEquals(expectedUserLocation, Locations.findBySessionId(sessionId1));
    }
    {
      // User 2
      Point expectedUserLocation = new Point(Bytes.bytesToInt(new byte[]{0, 0, 1, 0}), Bytes.bytesToInt(new byte[]{0, 0, 0, 1}));
      assertEquals(expectedUserLocation, Locations.findBySessionId(sessionId2));
    }

    assertEquals(2, Locations.size());
  }

  @Test
  public void testLocation_updateLocation() {
    SessionId sessionId1 = createNewSession();
    byte[] sessionIdBytes1 = new byte[8];
    sessionId1.readBytes(sessionIdBytes1);

    EmbeddedChannel channel = createChannel();
    byte[] data = new byte[]{Headers.Command.LOCATION.code, sessionIdBytes1[0], sessionIdBytes1[1], sessionIdBytes1[2], sessionIdBytes1[3], sessionIdBytes1[4], sessionIdBytes1[5], sessionIdBytes1[6], sessionIdBytes1[7],
        1, 2, 3, 4, 1, 2, 3, 4};
    channel.writeInbound(Unpooled.wrappedBuffer(data));
    assertNull(channel.readOutbound());

    {
      // Update
      data = new byte[]{Headers.Command.LOCATION.code, sessionIdBytes1[0], sessionIdBytes1[1], sessionIdBytes1[2], sessionIdBytes1[3], sessionIdBytes1[4], sessionIdBytes1[5], sessionIdBytes1[6], sessionIdBytes1[7],
          2, 2, 2, 2, 4, 4, 4, 4};
      channel.writeInbound(Unpooled.wrappedBuffer(data));
      assertNull(channel.readOutbound());

      Point expectedUserLocation = new Point(Bytes.bytesToInt(new byte[]{2, 2, 2, 2}), Bytes.bytesToInt(new byte[]{4, 4, 4, 4}));
      assertEquals(expectedUserLocation, Locations.findBySessionId(sessionId1));
      assertEquals(1, Locations.size());
    }
    {
      // Update but same location
      data = new byte[]{Headers.Command.LOCATION.code, sessionIdBytes1[0], sessionIdBytes1[1], sessionIdBytes1[2], sessionIdBytes1[3], sessionIdBytes1[4], sessionIdBytes1[5], sessionIdBytes1[6], sessionIdBytes1[7],
          2, 2, 2, 2, 4, 4, 4, 4};
      channel.writeInbound(Unpooled.wrappedBuffer(data));
      assertNull(channel.readOutbound());

      Point expectedUserLocation = new Point(Bytes.bytesToInt(new byte[]{2, 2, 2, 2}), Bytes.bytesToInt(new byte[]{4, 4, 4, 4}));
      assertEquals(expectedUserLocation, Locations.findBySessionId(sessionId1));
      assertEquals(1, Locations.size());
    }
  }

  @Test
  public void testMessageAndExit() {
    SessionId sessionId1 = createNewSession();
    sessionId1.incrementClientSequenceNo();
    byte[] sessionIdBytes1 = new byte[8];
    sessionId1.readBytes(sessionIdBytes1);

    SessionId sessionId2 = createNewSession();
    byte[] sessionIdBytes2 = new byte[8];
    sessionId2.readBytes(sessionIdBytes2);

    EmbeddedChannel channel = createChannel();
    byte[] data = new byte[]{Headers.Command.LOCATION.code, sessionIdBytes1[0], sessionIdBytes1[1], sessionIdBytes1[2], sessionIdBytes1[3], sessionIdBytes1[4], sessionIdBytes1[5], sessionIdBytes1[6], sessionIdBytes1[7],
        1, 2, 3, 4, 1, 2, 3, 4};
    channel.writeInbound(Unpooled.wrappedBuffer(data));
    data = new byte[]{Headers.Command.LOCATION.code, sessionIdBytes2[0], sessionIdBytes2[1], sessionIdBytes2[2], sessionIdBytes2[3], sessionIdBytes2[4], sessionIdBytes2[5], sessionIdBytes2[6], sessionIdBytes2[7],
        1, 2, 3, 4, 1, 2, 3, 4};
    channel.writeInbound(Unpooled.wrappedBuffer(data));
    assertNull(channel.readOutbound());
    byte[] tag = new byte[]{3, 2, 0, 1};
    byte[] displayNameBytes = "眞田𠀋助".getBytes(Config.CHARSET_MESSAGE);
    data = new byte[]{Headers.Command.TEXT.code, sessionIdBytes1[0], sessionIdBytes1[1], sessionIdBytes1[2], sessionIdBytes1[3], sessionIdBytes1[4], sessionIdBytes1[5], sessionIdBytes1[6], sessionIdBytes1[7],
        tag[0], tag[1], tag[2], tag[3], MessageCommands.areaKM(10), (byte) displayNameBytes.length};
    channel.writeInbound(Unpooled.unmodifiableBuffer(Unpooled.wrappedBuffer(data), Unpooled.wrappedBuffer(displayNameBytes), Unpooled.wrappedBuffer("はじめまして🙌".getBytes(Config.CHARSET_MESSAGE))));

    assertNotNull(Locations.findBySessionId(sessionId2));

    DatagramPacket received = channel.readOutbound();
    assertEquals(Unpooled.wrappedBuffer(new byte[]{Headers.mask(Headers.Control.RECEIVED.code, Headers.Command.TEXT.code), tag[0], tag[1], tag[2], tag[3], 0, 0, 0, 1}), received.content());

    DatagramPacket message = channel.readOutbound();
    sessionId2.incrementServerSequenceNo();
    sessionId2.readBytes(sessionIdBytes2);
    assertEquals(Unpooled.unmodifiableBuffer(Unpooled.wrappedBuffer(
        new byte[]{Headers.Command.TEXT.code, sessionIdBytes2[6], sessionIdBytes2[7], sessionIdBytes1[0], sessionIdBytes1[1], sessionIdBytes1[2], sessionIdBytes1[3],
            1, 2, 3, 4, 1, 2, 3, 4, (byte) displayNameBytes.length, MessageCommands.areaKM(10)}),
        Unpooled.wrappedBuffer(displayNameBytes), Unpooled.wrappedBuffer("はじめまして🙌".getBytes(Config.CHARSET_MESSAGE))), message.content());

    channel.writeInbound(Unpooled.wrappedBuffer(new byte[]{Headers.Control.EXIT.code, sessionIdBytes2[0], sessionIdBytes2[1], sessionIdBytes2[2], sessionIdBytes2[3], sessionIdBytes2[4], sessionIdBytes2[5], sessionIdBytes2[6], sessionIdBytes2[7]}));
    assertNull(channel.readOutbound());

    assertNull(Locations.findBySessionId(sessionId2));

    sessionId1.incrementClientSequenceNo();
    sessionId1.readBytes(sessionIdBytes1);

    tag[2] = 127; // Update tag.
    data = new byte[]{Headers.Command.TEXT.code, sessionIdBytes1[0], sessionIdBytes1[1], sessionIdBytes1[2], sessionIdBytes1[3], sessionIdBytes1[4], sessionIdBytes1[5], sessionIdBytes1[6], sessionIdBytes1[7],
        tag[0], tag[1], tag[2], tag[3], MessageCommands.areaKM(10), (byte) displayNameBytes.length};
    channel.writeInbound(Unpooled.unmodifiableBuffer(Unpooled.wrappedBuffer(data), Unpooled.wrappedBuffer(displayNameBytes), Unpooled.wrappedBuffer("はじめまして🙌".getBytes(Config.CHARSET_MESSAGE))));
    received = channel.readOutbound();
    assertEquals(Unpooled.wrappedBuffer(new byte[]{Headers.mask(Headers.Control.RECEIVED.code, Headers.Command.TEXT.code), tag[0], tag[1], tag[2], tag[3], 0, 0, 0, 0}), received.content());
    assertNull(channel.readOutbound());
  }
}
