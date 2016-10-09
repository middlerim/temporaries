package com.middlerim.server.channel;

import com.middlerim.message.Outbound;
import com.middlerim.server.message.OutboundMessage;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;

public class OutboundHandler extends ChannelOutboundHandlerAdapter {

  @Override
  public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
    @SuppressWarnings("unchecked")
    OutboundMessage<Outbound> outboundMessage = (OutboundMessage<Outbound>) msg;
    try {
      ChannelFuture f = outboundMessage.processOutput(ctx);
      f.syncUninterruptibly();
      if (f.isSuccess()) {
        promise.setFailure(f.cause());
      } else {
        promise.setSuccess();
      }
    } catch(Exception e) {
      promise.setFailure(e);
    }
  }

  @Override
  public boolean isSharable() {
    return true;
  }
}
