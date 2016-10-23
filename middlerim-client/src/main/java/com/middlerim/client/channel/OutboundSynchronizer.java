package com.middlerim.client.channel;

import java.util.ArrayDeque;
import java.util.Timer;
import java.util.TimerTask;

import com.middlerim.client.CentralEvents;
import com.middlerim.client.Config;
import com.middlerim.client.message.Markers;
import com.middlerim.client.session.Sessions;
import com.middlerim.client.view.ViewContext;
import com.middlerim.client.view.ViewEvents;
import com.middlerim.message.Outbound;
import com.middlerim.message.SequentialMessage;
import com.middlerim.session.Session;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;

public class OutboundSynchronizer extends ChannelOutboundHandlerAdapter {
  private static final String TAG = "OutSync";

  // Queue for sequential messages.
  static final ArrayDeque<MessageAndContext> MESSAGE_QUEUE = new ArrayDeque<>(Config.MAX_MESSAGE_QUEUE);

  private final ViewContext viewContext;
  private Timer retryTask = new Timer("OutboundMessage-retry", true);
  private long totalMsgSent = 1;
  private long startTimeMillis = System.currentTimeMillis();

  public OutboundSynchronizer(ViewContext viewContext) {
    this.viewContext = viewContext;
    initializeEventHandlers();
  }

  private void initializeEventHandlers() {
    CentralEvents.onReceived(new CentralEvents.Listener<CentralEvents.ReceivedEvent>() {
      @Override
      public void handle(CentralEvents.ReceivedEvent event) {
        synchronized (MESSAGE_QUEUE) {
          MESSAGE_QUEUE.poll(); // Remove first.
        }
        totalMsgSent++;
        sendFromQueue();
      }
    });
    ViewEvents.onResume(new ViewEvents.Listener<ViewEvents.ResumeEvent>() {
      @Override
      public void handle(ViewEvents.ResumeEvent event) {
        retryTask.schedule(new RetryTimerTask(null, 0), Config.MIN_RETRY_RERIOD_MILLIS);
      }
    });
    ViewEvents.onPause(new ViewEvents.Listener<ViewEvents.PauseEvent>() {
      @Override
      public void handle(ViewEvents.PauseEvent event) {
        retryTask.cancel();
      }
    });
  }

  private ChannelFuture sendFromQueue() {
    MessageAndContext next = MESSAGE_QUEUE.peek();
    if (next == null) {
      return null;
    }
    Session recipient = Sessions.getSession();
    recipient.sessionId.incrementClientSequenceNo();
    viewContext.logger().debug(TAG, "Sending the message " + next.message);
    return next.context.writeAndFlush(next.message);
  }

  public long averageMessageSentMillis() {
    return (System.currentTimeMillis() - startTimeMillis) / totalMsgSent;
  }

  private class RetryTimerTask extends TimerTask {
    private final MessageAndContext prev;
    private final int retryCount;

    RetryTimerTask(MessageAndContext prev, int retryCount) {
      this.prev = prev;
      this.retryCount = retryCount;
    }

    private boolean ensureNotAnonymous() {
      Session session = Sessions.getSession();
      if (session != null && !session.isNotAssigned()) {
        return true;
      }
      MessageAndContext mc = prev;
      if (mc == null) {
        mc = MESSAGE_QUEUE.peek();
      }
      if (mc == null) {
        return false;
      }
      if (mc.message == Markers.ASSIGN_AID) {
        return true;
      }
      mc.context.write(Markers.ASSIGN_AID);
      return false;
    }

    @Override
    public void run() {
      if (!ensureNotAnonymous()) {
        retryTask.schedule(new RetryTimerTask(prev, retryCount + 1), Config.MIN_RETRY_RERIOD_MILLIS);
        return;
      }
      MessageAndContext next = MESSAGE_QUEUE.peek();
      if (prev == null || prev != next) {
        long wait = averageMessageSentMillis();
        if (wait < Config.MIN_RETRY_RERIOD_MILLIS) {
          wait = Config.MIN_RETRY_RERIOD_MILLIS;
        } else if (wait > Config.MAX_RETRY_RERIOD_MILLIS) {
          wait = Config.MAX_RETRY_RERIOD_MILLIS;
        }
        viewContext.logger().debug(TAG, "Central server is working well. Next retry-check after " + wait + "millis");
        retryTask.schedule(new RetryTimerTask(next, 0), wait);
        return;
      }
      // Retry
      long wait = retryCount * (long) Math.pow(2, retryCount) * 1000 + Config.MIN_RETRY_RERIOD_MILLIS;
      wait = wait <= Config.MAX_RETRY_RERIOD_MILLIS ? wait : Config.MAX_RETRY_RERIOD_MILLIS;
      Session session = Sessions.getSession();
      viewContext.logger().debug(TAG, "Central server seems to stop. Wait for re-sending the message " + session + " for " + wait + "millis");

      prev.context.write(prev.message);
      retryTask.schedule(new RetryTimerTask(prev, retryCount + 1), wait);
    }
  }

  @Override
  public void write(ChannelHandlerContext ctx, Object msg, final ChannelPromise promise) throws Exception {
    Outbound message = (Outbound) msg;

    if (message == Markers.AGAIN) {
      ChannelFuture againFuture = sendFromQueue();
      if (againFuture == null) {
        viewContext.logger().warn(TAG, "Central Server request sending a message again, but the client doesn't have any message...");
        promise.setSuccess();
      }
      againFuture.addListener(new ChannelFutureListener() {
        @Override
        public void operationComplete(ChannelFuture future) throws Exception {
          if (future.isSuccess()) {
            promise.setSuccess();
          } else {
            promise.setFailure(future.cause());
          }
        }
      });
      return;
    }
    if (message instanceof SequentialMessage) {
      if (MESSAGE_QUEUE.size() >= Config.MAX_MESSAGE_QUEUE) {
        // Message queue is full.
        CentralEvents.fireLostMessage(message);
        return;
      }
      boolean added;
      synchronized (MESSAGE_QUEUE) {
        added = MESSAGE_QUEUE.offer(new MessageAndContext(message, ctx));
      }
      if (!added) {
        CentralEvents.fireLostMessage(message);
        return;
      }
      sendFromQueue();
      return;
    } else {
      if (Sessions.getSession().isNotAssigned()) {
        // Currently the client is working on connecting with central server.
        viewContext.logger().warn(TAG, "Discarded the message" + message + " since the session has not been created yet.");
        ctx.write(Markers.ASSIGN_AID);
        return;
      } else {
        ctx.writeAndFlush(message);
        return;
      }
    }
  }

  @Override
  public boolean isSharable() {
    return true;
  }

  private static final class MessageAndContext {
    final Outbound message;
    final ChannelHandlerContext context;

    MessageAndContext(Outbound message, ChannelHandlerContext context) {
      this.message = message;
      this.context = context;
    }
  }
}
