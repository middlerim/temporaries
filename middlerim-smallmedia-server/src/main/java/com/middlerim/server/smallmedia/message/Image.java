package com.middlerim.server.smallmedia.message;

import java.nio.ByteBuffer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.middlerim.message.Inbound;
import com.middlerim.message.Outbound;
import com.middlerim.server.Headers;
import com.middlerim.server.smallmedia.channel.AttributeKeys;
import com.middlerim.server.smallmedia.storage.Images;
import com.middlerim.session.Session;
import com.middlerim.storage.persistent.Persistent;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.DatagramPacket;

public class Image {
  private static final Logger LOG = LoggerFactory.getLogger(Image.class);

  public static class ImageReceived implements Outbound {

    private static final int FIXED_BYTE_SIZE = 9;

    private final int tag;
    private final int numberOfDelivery;
    private ImageReceived(int tag, int numberOfDelivery) {
      this.tag = tag;
      this.numberOfDelivery = numberOfDelivery;
    }

    @Override
    public ChannelFuture processOutput(ChannelHandlerContext ctx, Session recipient) {
      return ctx.writeAndFlush(new DatagramPacket(ctx.alloc().buffer(FIXED_BYTE_SIZE, FIXED_BYTE_SIZE)
          .writeByte(Headers.SmallMedia.IMAGE_RECEIVED.code)
          .writeInt(tag)
          .writeInt(numberOfDelivery), recipient.address));
    }

    @Override
    public int byteSize() {
      return FIXED_BYTE_SIZE;
    }
  }

  public static class In implements Inbound, Persistent<In> {
    public static final int SERIALIZED_BYTE_SIZE = Byte.BYTES + Integer.BYTES * 2;

    public final int tag;
    public final ByteBuf data;

    public In(int tag, ByteBuf data) {
      this.tag = tag;
      this.data = data;
    }

    @Override
    public void processInput(ChannelHandlerContext ctx) {
      try {
        Session session = ctx.channel().attr(AttributeKeys.SESSION).get();
        Images.putFragment(session.sessionId.userId(), tag, this);
      } finally {
        data.release();
      }
    }

    @Override
    public long id() {
      return 0;
    }

    @Override
    public void read(ByteBuffer buf) {
      // TODO Auto-generated method stub
    }

    @Override
    public void write(ByteBuffer buf) {
      // TODO Auto-generated method stub
    }
  }

}
