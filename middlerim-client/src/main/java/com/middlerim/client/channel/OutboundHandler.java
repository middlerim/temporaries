package com.middlerim.client.channel;

import com.middlerim.client.message.OutboundMessage;
import com.middlerim.client.session.Sessions;
import com.middlerim.client.storage.MessageStorage;
import com.middlerim.message.Outbound;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;

public class OutboundHandler extends ChannelOutboundHandlerAdapter {

  @Override
  public void write(ChannelHandlerContext ctx, Object msg, final ChannelPromise promise) throws Exception {
    @SuppressWarnings("unchecked")
    OutboundMessage<Outbound> outboundMessage = (OutboundMessage<Outbound>) msg;
    MessageStorage.putMessage(Sessions.getSession().sessionId.sequenceNo(), outboundMessage.message);
    outboundMessage.processOutput(ctx).addListener(new ChannelFutureListener() {
      @Override
      public void operationComplete(ChannelFuture future) throws Exception {
        if (future.isSuccess()) {
          promise.setSuccess();
        } else {
          promise.setFailure(future.cause());
        }
      }
    });
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
    super.exceptionCaught(ctx, cause);
  }

  @Override
  public boolean isSharable() {
    return true;
  }
}
