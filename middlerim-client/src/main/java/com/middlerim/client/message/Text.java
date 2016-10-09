package com.middlerim.client.message;

import java.nio.ByteBuffer;

import com.middlerim.client.CentralEvents;
import com.middlerim.message.Inbound;
import com.middlerim.message.Outbound;
import com.middlerim.server.Headers;
import com.middlerim.session.Session;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.DatagramPacket;

public class Text implements Inbound, Outbound {

  public final ByteBuffer messageBytes;
  public final byte messageCommand;

  public Text(ByteBuffer messageBytes, byte messageCommand) {
    this.messageBytes = messageBytes;
    this.messageCommand = messageCommand;
  }

  @Override
  public void processInput(ChannelHandlerContext ctx) {
    CentralEvents.fireReceiveMessage(messageBytes);
  }

  @Override
  public ChannelFuture processOutput(ChannelHandlerContext ctx, Session session) {
    byte[] sessionIdBytes = new byte[8];
    session.sessionId.readBytes(sessionIdBytes);
    int byteSize = byteSize();
    return ctx.writeAndFlush(new DatagramPacket(
        ctx.alloc().buffer(byteSize, byteSize)
            .writeByte(Headers.mask(Headers.TEXT, Headers.COMPLETE))
            .writeBytes(sessionIdBytes)
            .writeByte(messageCommand)
            .writeBytes(messageBytes),
        session.address));
  }

  @Override
  public int byteSize() {
    return 10 + messageBytes.remaining();
  }
}
