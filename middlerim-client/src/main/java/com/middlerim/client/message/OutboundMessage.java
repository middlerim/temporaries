package com.middlerim.client.message;

import com.middlerim.message.ControlMessage;
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
    if (!(message instanceof ControlMessage)) {
      recipient.sessionId.incrementSequenceNo();
    }
    return message.processOutput(ctx, recipient);
  }
}
