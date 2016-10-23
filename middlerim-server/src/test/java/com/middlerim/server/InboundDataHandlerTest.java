package com.middlerim.server;

import static org.junit.Assert.*;

import java.net.InetSocketAddress;

import org.junit.Before;
import org.junit.Test;

import com.middlerim.location.Point;
import com.middlerim.server.channel.InboundHandler;
import com.middlerim.server.channel.OutboundHandler;
import com.middlerim.server.channel.PacketToInboundDecoder;
import com.middlerim.server.storage.Locations;
import com.middlerim.server.storage.Sessions;
import com.middlerim.session.SessionId;
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
    EmbeddedChannel channel = new EmbeddedChannel(new TestInboudDecoder(), new PacketToInboundDecoder(), new InboundHandler(), new OutboundHandler());
    return channel;
  }

  private EmbeddedChannel createChannel(InetSocketAddress recipient, InetSocketAddress sender) {
    EmbeddedChannel channel = new EmbeddedChannel(new TestInboudDecoder(recipient, sender), new PacketToInboundDecoder(), new InboundHandler(), new OutboundHandler());
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

  @Test
  public void testException_invalidData() {
    EmbeddedChannel channel = createChannel();
    byte[] data = new byte[]{Headers.LOCATION, 0, 0, 0, 1, 0, 3, 0, 2};
    channel.writeInbound(Unpooled.wrappedBuffer(data));
    DatagramPacket result = channel.readOutbound();

    assertEquals(Unpooled.wrappedBuffer(new byte[]{Headers.AGAIN, 3}), result.content());
    assertEquals(44444, result.recipient().getPort());
  }

  @Test
  public void testReceived() {
    EmbeddedChannel channel = createChannel();
    byte[] data = new byte[]{Headers.RECEIVED, 0, 0, 0, 1, 0, 0, 7, 8};
    channel.writeInbound(Unpooled.wrappedBuffer(data));
    Object result = channel.readOutbound();
    assertNull(result);
  }

  @Test
  public void testLocation() {
    EmbeddedChannel channel = createChannel();
    byte[] data = new byte[]{Headers.LOCATION, 0, 0, 0, 1, 0, 0, 7, 8, 1, 2, 3, 4, 1, 2, 3, 4};
    channel.writeInbound(Unpooled.wrappedBuffer(data));

    assertNull(channel.readOutbound());
    SessionId sessionId = new SessionId(new byte[]{0, 0, 0, 1, 0, 0, 7, 8});
    Point expectedUserLocation = new Point(Bytes.bytesToInt(new byte[]{1, 2, 3, 4}), Bytes.bytesToInt(new byte[]{1, 2, 3, 4}));
    assertEquals(expectedUserLocation, Locations.findBySessionId(sessionId));
    assertEquals(1, Locations.size());
  }

  @Test
  public void testLocation_2users() {
    {
      EmbeddedChannel channel = createChannel();
      byte[] user1 = new byte[]{Headers.LOCATION, 0, 0, 0, 1, 0, 0, 7, 8, 1, 2, 3, 4, 1, 2, 3, 4};
      channel.writeInbound(Unpooled.wrappedBuffer(user1));
    }
    {
      EmbeddedChannel channel = createChannel();
      byte[] user2 = new byte[]{Headers.LOCATION, 0, 0, 0, 2, 0, 0, 2, 1, 2, 2, 2, 2, 4, 4, 4, 4};
      channel.writeInbound(Unpooled.wrappedBuffer(user2));
    }
    {
      // User 1
      SessionId sessionId = new SessionId(new byte[]{0, 0, 0, 1, 0, 0, 7, 8});
      Point expectedUserLocation = new Point(Bytes.bytesToInt(new byte[]{1, 2, 3, 4}), Bytes.bytesToInt(new byte[]{1, 2, 3, 4}));
      assertEquals(expectedUserLocation, Locations.findBySessionId(sessionId));
    }
    {
      // User 2
      SessionId sessionId = new SessionId(new byte[]{0, 0, 0, 2, 0, 0, 2, 1});
      Point expectedUserLocation = new Point(Bytes.bytesToInt(new byte[]{2, 2, 2, 2}), Bytes.bytesToInt(new byte[]{4, 4, 4, 4}));
      assertEquals(expectedUserLocation, Locations.findBySessionId(sessionId));
    }

    assertEquals(2, Locations.size());
  }

  @Test
  public void testLocation_2users_sameLocation() {
    {
      EmbeddedChannel channel = createChannel();
      byte[] user1 = new byte[]{Headers.LOCATION, 0, 0, 0, 1, 0, 0, 7, 8, 1, 2, 3, 4, 1, 2, 3, 4};
      channel.writeInbound(Unpooled.wrappedBuffer(user1));
    }
    {
      EmbeddedChannel channel = createChannel();
      byte[] user2 = new byte[]{Headers.LOCATION, 0, 0, 0, 2, 0, 0, 2, 0, 0, 0, 1, 0, 0, 0, 0, 1};
      channel.writeInbound(Unpooled.wrappedBuffer(user2));
    }
    {
      // User 1
      SessionId sessionId = new SessionId(new byte[]{0, 0, 0, 1, 0, 0, 7, 8});
      Point expectedUserLocation = new Point(Bytes.bytesToInt(new byte[]{1, 2, 3, 4}), Bytes.bytesToInt(new byte[]{1, 2, 3, 4}));
      assertEquals(expectedUserLocation, Locations.findBySessionId(sessionId));
    }
    {
      // User 2
      SessionId sessionId = new SessionId(new byte[]{0, 0, 0, 2, 0, 0, 2, 1});
      Point expectedUserLocation = new Point(Bytes.bytesToInt(new byte[]{0, 0, 1, 0}), Bytes.bytesToInt(new byte[]{0, 0, 0, 1}));
      assertEquals(expectedUserLocation, Locations.findBySessionId(sessionId));
    }

    assertEquals(2, Locations.size());
  }

  @Test
  public void testLocation_updateLocation() {
    EmbeddedChannel channel = createChannel();
    byte[] data = new byte[]{Headers.LOCATION, 0, 0, 0, 1, 0, 0, 7, 8, 1, 2, 3, 4, 1, 2, 3, 4};
    channel.writeInbound(Unpooled.wrappedBuffer(data));
    assertNull(channel.readOutbound());

    {
      // Update
      data = new byte[]{Headers.LOCATION, 0, 0, 0, 1, 0, 0, 7, 9, 2, 2, 2, 2, 4, 4, 4, 4};
      channel.writeInbound(Unpooled.wrappedBuffer(data));
      assertNull(channel.readOutbound());

      SessionId sessionId = new SessionId(new byte[]{0, 0, 0, 1, 0, 0, 7, 8});
      Point expectedUserLocation = new Point(Bytes.bytesToInt(new byte[]{2, 2, 2, 2}), Bytes.bytesToInt(new byte[]{4, 4, 4, 4}));
      assertEquals(expectedUserLocation, Locations.findBySessionId(sessionId));
      assertEquals(1, Locations.size());
    }
    {
      // Update but same location
      data = new byte[]{Headers.LOCATION, 0, 0, 0, 1, 0, 0, 7, 10, 2, 2, 2, 2, 4, 4, 4, 4};
      channel.writeInbound(Unpooled.wrappedBuffer(data));
      assertNull(channel.readOutbound());

      SessionId sessionId = new SessionId(new byte[]{0, 0, 0, 1, 0, 0, 7, 8});
      Point expectedUserLocation = new Point(Bytes.bytesToInt(new byte[]{2, 2, 2, 2}), Bytes.bytesToInt(new byte[]{4, 4, 4, 4}));
      assertEquals(expectedUserLocation, Locations.findBySessionId(sessionId));
      assertEquals(1, Locations.size());
    }
  }

  @Test
  public void testMessage() {
    EmbeddedChannel channel1 = createChannel();
    {
      byte[] data = new byte[]{Headers.LOCATION, 0, 0, 0, 1, 0, 2, 7, 8, 1, 2, 3, 4, 1, 2, 3, 4};
      channel1.writeInbound(Unpooled.wrappedBuffer(data));
      assertNull(channel1.readOutbound());
    }
    EmbeddedChannel channel2 = createChannel(DatagramSocketAddress.createUnresolved("localhost", 55554), DatagramSocketAddress.createUnresolved("localhost", 55555));
    {
      byte[] data = new byte[]{Headers.LOCATION, 0, 0, 0, 2, 0, 4, 7, 8, 1, 2, 3, 4, 1, 2, 3, 4};
      channel2.writeInbound(Unpooled.wrappedBuffer(data));
      assertNull(channel2.readOutbound());
    }
    byte[] displayNameBytes = "佐助✌".getBytes(Config.CHARSET_MESSAGE);
    byte[] data = new byte[]{Headers.mask(Headers.TEXT, Headers.COMPLETE), 0, 0, 0, 1, 0, 3, 7, 8, MessageCommands.areaKM(10), (byte) displayNameBytes.length};
    channel1.writeInbound(Unpooled.unmodifiableBuffer(Unpooled.wrappedBuffer(data), Unpooled.wrappedBuffer(displayNameBytes), Unpooled.wrappedBuffer("はじめまして🙌".getBytes(Config.CHARSET_MESSAGE))));
    assertNull(channel2.readOutbound());

    DatagramPacket received = channel1.readOutbound();
    assertEquals(Unpooled.wrappedBuffer(new byte[]{Headers.mask(Headers.RECEIVED, Headers.TEXT), 3, 0, 0, 0, 1}), received.content());
    assertEquals(44444, received.recipient().getPort());

    DatagramPacket message = channel1.readOutbound();
    assertEquals(Unpooled.unmodifiableBuffer(Unpooled.wrappedBuffer(new byte[]{Headers.mask(Headers.TEXT, Headers.COMPLETE), 7, 9, 0, 0, 0, 1, 1, 2, 3, 4, 1, 2, 3, 4, (byte) displayNameBytes.length, MessageCommands.areaKM(10)}),
        Unpooled.wrappedBuffer(displayNameBytes),
        Unpooled.wrappedBuffer("はじめまして🙌".getBytes(Config.CHARSET_MESSAGE))), message.content());
    assertEquals(55555, message.recipient().getPort());

  }

  @Test
  public void testExit() {
    EmbeddedChannel channel = createChannel();
    byte[] data = new byte[]{Headers.LOCATION, 0, 0, 0, 1, 0, 4, 0, 8, 1, 2, 3, 4, 1, 2, 3, 4};
    channel.writeInbound(Unpooled.wrappedBuffer(data));
    data = new byte[]{Headers.LOCATION, 0, 0, 0, 2, 0, 6, 2, 1, 1, 2, 3, 4, 1, 2, 3, 4};
    channel.writeInbound(Unpooled.wrappedBuffer(data));
    assertNull(channel.readOutbound());
    byte[] displayNameBytes = "佐助✌".getBytes(Config.CHARSET_MESSAGE);
    data = new byte[]{Headers.mask(Headers.TEXT, Headers.COMPLETE), 0, 0, 0, 1, 0, 4, 0, 8, MessageCommands.areaKM(10), (byte) displayNameBytes.length};
    channel.writeInbound(Unpooled.unmodifiableBuffer(Unpooled.wrappedBuffer(data), Unpooled.wrappedBuffer(displayNameBytes), Unpooled.wrappedBuffer("はじめまして🙌".getBytes(Config.CHARSET_MESSAGE))));

    SessionId sessionId = new SessionId(new byte[]{0, 0, 0, 2, 0, 0, 7, 8});
    assertNotNull(Locations.findBySessionId(sessionId));

    DatagramPacket received = channel.readOutbound();
    assertEquals(Unpooled.wrappedBuffer(new byte[]{Headers.mask(Headers.RECEIVED, Headers.TEXT), 4, 0, 0, 0, 1}), received.content());

    DatagramPacket message = channel.readOutbound();
    assertEquals(Unpooled.unmodifiableBuffer(Unpooled.wrappedBuffer(
        new byte[]{Headers.mask(Headers.TEXT, Headers.COMPLETE), 2, 2, 0, 0, 0, 1, 1, 2, 3, 4, 1, 2, 3, 4, (byte) displayNameBytes.length, MessageCommands.areaKM(10)}),
        Unpooled.wrappedBuffer(displayNameBytes), Unpooled.wrappedBuffer("はじめまして🙌".getBytes(Config.CHARSET_MESSAGE))), message.content());

    channel.writeInbound(Unpooled.wrappedBuffer(new byte[]{Headers.EXIT, 0, 0, 0, 2, 0, 0, 7, 8}));
    assertNull(channel.readOutbound());

    assertNull(Locations.findBySessionId(sessionId));

    data = new byte[]{Headers.mask(Headers.TEXT, Headers.COMPLETE), 0, 0, 0, 1, 0, 5, 0, 8, MessageCommands.areaKM(10)};
    channel.writeInbound(Unpooled.unmodifiableBuffer(Unpooled.wrappedBuffer(data), Unpooled.wrappedBuffer("はじめまして🙌".getBytes(Config.CHARSET_MESSAGE))));
    received = channel.readOutbound();
    assertEquals(Unpooled.wrappedBuffer(new byte[]{Headers.mask(Headers.RECEIVED, Headers.TEXT), 5, 0, 0, 0, 0}), received.content());
    assertNull(channel.readOutbound());
  }
}
