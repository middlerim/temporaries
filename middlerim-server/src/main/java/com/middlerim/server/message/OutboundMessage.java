package com.middlerim.server.message;

import com.middlerim.message.Outbound;
import com.middlerim.session.Session;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;

public class OutboundMessage<M extends Outbound> {
  public final Session recipient;
  public final M message;
  public OutboundMessage(Session recipient, M message) {
    this.recipient = recipient;
    this.message = message;
  }

  public ChannelFuture processOutput(ChannelHandlerContext ctx) {
    return message.processOutput(ctx, recipient);
  }
}
