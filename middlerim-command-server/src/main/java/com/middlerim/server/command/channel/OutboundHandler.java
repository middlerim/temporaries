package com.middlerim.server.command.channel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.middlerim.message.Outbound;
import com.middlerim.server.command.message.OutboundMessage;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;

public class OutboundHandler extends ChannelOutboundHandlerAdapter {
  private static final Logger LOG = LoggerFactory.getLogger(OutboundHandler.class);

  @Override
  public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
    @SuppressWarnings("unchecked")
    OutboundMessage<Outbound> outboundMessage = (OutboundMessage<Outbound>) msg;
    try {
      LOG.debug("Outbound - {}: {}", outboundMessage.recipient, outboundMessage.message);
      ChannelFuture f = outboundMessage.processOutput(ctx);
      f.syncUninterruptibly();
      if (f.isSuccess()) {
        promise.setFailure(f.cause());
      } else {
        promise.setSuccess();
      }
    } catch (Exception e) {
      promise.setFailure(e);
    }
  }

  @Override
  public boolean isSharable() {
    return true;
  }
}
