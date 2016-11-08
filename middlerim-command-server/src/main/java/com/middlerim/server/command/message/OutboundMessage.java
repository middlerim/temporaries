package com.middlerim.server.command.message;

import com.middlerim.message.SequentialMessage;
import com.middlerim.server.command.storage.Messages;
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
    if (message instanceof SequentialMessage) {
      Messages.removeMessage(recipient.sessionId.userId(), recipient.sessionId.serverSequenceNo());
      short serverSequenceNo = recipient.sessionId.incrementServerSequenceNo();
      Messages.putMessage(recipient.sessionId.userId(), serverSequenceNo, message);
    }
    return message.processOutput(ctx, recipient);
  }
}
