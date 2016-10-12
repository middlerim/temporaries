package com.middlerim.client.message;

import com.middlerim.client.CentralEvents;
import com.middlerim.client.session.Sessions;
import com.middlerim.message.ControlMessage;
import com.middlerim.message.Outbound;
import com.middlerim.session.Session;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;

public class OutboundMessage<M extends Outbound> {
  public final Session recipient;
  public final M message;

  static {
    CentralEvents.onReceived(new CentralEvents.Listener<CentralEvents.ReceivedEvent>() {
      @Override
      public void handle(CentralEvents.ReceivedEvent event) {
        Messages.removeSentMessage(event.sequenceNo);
      }
    });
  }

  public OutboundMessage(Session recipient, M message) {
    this.recipient = recipient;
    this.message = message;
  }

  public ChannelFuture processOutput(ChannelHandlerContext ctx) {
    if (!(message instanceof ControlMessage)) {
      Messages.putMessage(Sessions.getSession().sessionId.sequenceNo(), this);
      recipient.sessionId.incrementSequenceNo();
    }
    return message.processOutput(ctx, recipient);
  }
}
