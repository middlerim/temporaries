package com.middlerim.server.smallmedia.channel;

import java.nio.BufferUnderflowException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.middlerim.server.Headers;
import com.middlerim.server.Headers.Header;
import com.middlerim.server.channel.PacketToInboundDecoder;
import com.middlerim.server.message.Markers;
import com.middlerim.server.message.OutboundMessage;
import com.middlerim.server.smallmedia.Config;
import com.middlerim.server.smallmedia.message.Image;
import com.middlerim.session.Session;
import com.middlerim.session.SessionId;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;

public class SmallMediaHandler extends PacketToInboundDecoder {
  private static final Logger LOG = LoggerFactory.getLogger(SmallMediaHandler.class);

  @Override
  public boolean isSharable() {
    return true;
  }

  @Override
  protected void acceptReceived(SessionId clientSessionId) {
    // TODO Auto-generated method stub

  }

  @Override
  protected boolean handleNonSequentialRequest(ChannelHandlerContext ctx, Header header, Session session, SessionId clientSessionId, ByteBuf in, List<Object> out) {
    return false;
  }

  @Override
  protected boolean handleSequentialRequest(ChannelHandlerContext ctx, Header header, int tag, Session session, SessionId clientSessionId, ByteBuf in, List<Object> out) {
    ctx.channel().attr(AttributeKeys.SESSION).set(session);

    if (header == Headers.SmallMedia.IMAGE) {
      LOG.debug("Packet[IMAGE] - {}: tag={}, buff={}", session, tag, in);
      out.add(new Image.In(tag, in.retain()));
      return true;
    }
    return false;
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
    ctx.channel().writeAndFlush(new OutboundMessage<>(ctx.channel().attr(AttributeKeys.SESSION).get(), Markers.INVALID_DATA));
    // Ignore BufferUnderFlowException since length of the packet isn't validated deliberately in advance.
    if (Config.TEST | !(cause instanceof BufferUnderflowException || (cause.getCause() != null && cause.getCause() instanceof BufferUnderflowException))) {
      LOG.error("Unexpected exception", cause);
    }
  }
}
