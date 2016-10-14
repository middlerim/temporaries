package com.middlerim.client.message;

import java.util.ArrayDeque;

import com.middlerim.client.CentralEvents;
import com.middlerim.client.Config;
import com.middlerim.client.session.Sessions;
import com.middlerim.message.Outbound;
import com.middlerim.message.SequentialMessage;
import com.middlerim.session.Session;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;

public class OutboundMessage<M extends Outbound> {
  // Queue for sequential messages.
  public static final ArrayDeque<Short> MESSAGE_QUEUE = new ArrayDeque<>(Config.MAX_MESSAGE_QUEUE);

  public final Session recipient;
  public final M message;

  private static ChannelHandlerContext context = null;
  private static int currentSendingMessageSequenceNo = 0;
  private static int retryCount = 0;

  static {
    CentralEvents.onReceived(new CentralEvents.Listener<CentralEvents.ReceivedEvent>() {
      @Override
      public void handle(CentralEvents.ReceivedEvent event) {
        synchronized (MESSAGE_QUEUE) {
          MESSAGE_QUEUE.remove(event.sequenceNo);
          Messages.removeMessage(event.sequenceNo);

          Short sequenceNo = MESSAGE_QUEUE.peek();
          if (sequenceNo == null) {
            return;
          }
          OutboundMessage<?> next = Messages.getMessage(sequenceNo);
          if (currentSendingMessageSequenceNo == sequenceNo) {
            if (++retryCount > Config.MAX_RETRY) {
              CentralEvents.fireLostMessage(next.recipient.sessionId.sequenceNo());
              retryCount = 0;
              return;
            }
          } else {
            currentSendingMessageSequenceNo = sequenceNo;
            retryCount = 1;
          }
          next.message.processOutput(context, next.recipient.copyWithNewSequenceNo(sequenceNo));
        }
      }
    });
  }

  public OutboundMessage(Session recipient, M message) {
    this.recipient = recipient;
    this.message = message;
  }

  public ChannelFuture processOutput(ChannelHandlerContext ctx) {
    synchronized (MESSAGE_QUEUE) {
      Short sequenceNo = null;
      if (!MESSAGE_QUEUE.isEmpty()) {
        System.out.println("Central server is not working.");
      }
      context = ctx;
      ChannelFuture result = null;
      sequenceNo = MESSAGE_QUEUE.peek();
      if (sequenceNo != null) {
        OutboundMessage<?> next = Messages.getMessage(sequenceNo);
        result = next.message.processOutput(ctx, next.recipient.copyWithNewSequenceNo(sequenceNo));
      }
      if (message instanceof SequentialMessage) {
        recipient.sessionId.incrementSequenceNo();
        Messages.putMessage(Sessions.getSession().sessionId.sequenceNo(), this);
        if (MESSAGE_QUEUE.size() > Config.MAX_MESSAGE_QUEUE
            || !MESSAGE_QUEUE.offer(recipient.sessionId.sequenceNo())) {
          // Message queue is full.
          CentralEvents.fireLostMessage(this.recipient.sessionId.sequenceNo());
          // Return success even it's kind of a error, but still an expecting behavior.
          result = ctx.newSucceededFuture();
        } else if (sequenceNo == null) {
          // Send this only when MESSAGE_QUEUE is empty.
          result = message.processOutput(ctx, recipient);
        }
      } else {
        if (recipient.isNotAssigned()) {
          // Currently working on connecting with central server.
          result = ctx.newSucceededFuture();
        } else {
          result = message.processOutput(ctx, recipient);
        }
      }
      if (result == null) {
        throw new IllegalStateException("Result must not be null.");
      }
      return result;
    }
  }
}
