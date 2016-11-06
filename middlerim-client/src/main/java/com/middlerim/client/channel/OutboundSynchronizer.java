package com.middlerim.client.channel;

import java.util.ArrayDeque;
import java.util.Timer;
import java.util.TimerTask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.middlerim.client.CentralEvents;
import com.middlerim.client.Config;
import com.middlerim.client.message.Markers;
import com.middlerim.client.session.Sessions;
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
  private static final Logger LOG = LoggerFactory.getLogger(Config.INTERNAL_APP_NAME);

  // Queue for sequential messages.
  static final ArrayDeque<MessageAndContext<SequentialMessage>> MESSAGE_QUEUE = new ArrayDeque<>(Config.MAX_MESSAGE_QUEUE);
  private final long startTimeMillis = System.currentTimeMillis();

  private Timer retryTimer;
  private long totalMsgSent = 1;
  private boolean working;
  private MessageAndContext<Outbound> lastPossiblyDiscardedMessage;

  public OutboundSynchronizer() {
    initializeEventHandlers();
  }

  private void initializeEventHandlers() {
    CentralEvents.onReceived("OutboundSynchronizer.CentralEvents.Listener<CentralEvents.ReceivedEvent>", new CentralEvents.Listener<CentralEvents.ReceivedEvent>() {
      @Override
      public void handle(CentralEvents.ReceivedEvent event) {
        working = true;
        synchronized (MESSAGE_QUEUE) {
          MESSAGE_QUEUE.poll(); // Remove first.
        }
        sendLastPossiblyDiscardedMessage();
        totalMsgSent++;
        sendFromQueue();
      }
    });
    ViewEvents.onResume("OutboundSynchronizer.ViewEvents.Listener<ViewEvents.ResumeEvent>", new ViewEvents.Listener<ViewEvents.ResumeEvent>() {
      @Override
      public void handle(ViewEvents.ResumeEvent event) {
        if (retryTimer != null) {
          retryTimer.cancel();
        }
        retryTimer = new Timer(Config.INTERNAL_APP_NAME + "-sync", true);
        retryTimer.schedule(new RetryTimerTask(MESSAGE_QUEUE.peek(), 0), Config.MIN_RETRY_RERIOD_MILLIS);
      }
    });
    ViewEvents.onPause("OutboundSynchronizer.ViewEvents.Listener<ViewEvents.PauseEvent>", new ViewEvents.Listener<ViewEvents.PauseEvent>() {
      @Override
      public void handle(ViewEvents.PauseEvent event) {
        if (retryTimer != null) {
          retryTimer.cancel();
          retryTimer = null;
        }
      }
    });
  }

  private void sendLastPossiblyDiscardedMessage() {
    if (lastPossiblyDiscardedMessage != null && !Sessions.getSession().isNotAssigned()) {
      lastPossiblyDiscardedMessage.context.writeAndFlush(lastPossiblyDiscardedMessage.message);
      lastPossiblyDiscardedMessage = null;
    }
  }

  private ChannelFuture sendFromQueue() {
    MessageAndContext<SequentialMessage> next = MESSAGE_QUEUE.peek();
    if (next == null) {
      return null;
    }
    Session recipient = Sessions.getSession();
    recipient.sessionId.incrementClientSequenceNo();
    LOG.debug("Sending the message " + next.message);
    return next.context.writeAndFlush(next.message);
  }

  public long averageMessageSentMillis() {
    return (System.currentTimeMillis() - startTimeMillis) / totalMsgSent;
  }

  private class RetryTimerTask extends TimerTask {
    private final MessageAndContext<SequentialMessage> prev;
    private final int retryCount;

    RetryTimerTask(MessageAndContext<SequentialMessage> prev, int retryCount) {
      this.prev = prev;
      this.retryCount = retryCount;
    }

    private boolean ensureNotAnonymous() {
      Session session = Sessions.getSession();
      if (session != null && !session.isNotAssigned()) {
        return true;
      }
      MessageAndContext<SequentialMessage> mc = prev;
      if (mc == null) {
        mc = MESSAGE_QUEUE.peek();
      }
      if (mc == null) {
        return false;
      }
      if (mc.message == Markers.ASSIGN_AID) {
        return true;
      }
      mc.context.writeAndFlush(Markers.ASSIGN_AID);
      return false;
    }

    @Override
    public void run() {
      if (!ensureNotAnonymous()) {
        retryTimer.schedule(new RetryTimerTask(prev, retryCount + 1), Config.MIN_RETRY_RERIOD_MILLIS);
        return;
      }
      MessageAndContext<SequentialMessage> next = MESSAGE_QUEUE.peek();
      if (prev == null || prev != next) {
        long wait = averageMessageSentMillis();
        if (wait < Config.MIN_RETRY_RERIOD_MILLIS) {
          wait = Config.MIN_RETRY_RERIOD_MILLIS;
        } else if (wait > Config.MAX_RETRY_RERIOD_MILLIS) {
          wait = Config.MAX_RETRY_RERIOD_MILLIS;
        }
        LOG.debug("Central server is working well. Next retry-check after " + wait + "millis");
        retryTimer.schedule(new RetryTimerTask(next, 0), wait);
        return;
      }
      // Retry
      working = false;
      long wait = retryCount * (long) Math.pow(2, retryCount) * 1000 + Config.MIN_RETRY_RERIOD_MILLIS;
      wait = wait <= Config.MAX_RETRY_RERIOD_MILLIS ? wait : Config.MAX_RETRY_RERIOD_MILLIS;
      Session session = Sessions.getSession();
      LOG.debug("Central server seems to stop. Wait for re-sending the message " + session + " for " + wait + "millis");

      prev.context.writeAndFlush(prev.message);
      retryTimer.schedule(new RetryTimerTask(prev, retryCount + 1), wait);
    }
  }

  @Override
  public void write(ChannelHandlerContext ctx, Object msg, final ChannelPromise promise) throws Exception {
    Outbound message = (Outbound) msg;

    if (message instanceof Markers.Again) {
      MessageAndContext<SequentialMessage> next = MESSAGE_QUEUE.peek();
      int requestedTag = ((Markers.Again)message).tag;
      if (next.message.tag() != requestedTag) {
        LOG.warn("Invalid request from central server. requested tag={}, current tag={}", requestedTag, next.message.tag());
        return;
      }
      ChannelFuture againFuture = sendFromQueue();
      if (againFuture == null) {
        LOG.debug("Central Server request sending a message again, but the client doesn't have any message...");
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
        CentralEvents.fireLostMessage((SequentialMessage) message, CentralEvents.LostMessageEvent.Type.LIMIT);
        return;
      }
      boolean added;
      synchronized (MESSAGE_QUEUE) {
        added = MESSAGE_QUEUE.offer(new MessageAndContext<>((SequentialMessage) message, ctx));
      }
      if (!added) {
        CentralEvents.fireLostMessage((SequentialMessage) message, CentralEvents.LostMessageEvent.Type.LIMIT);
        return;
      }
      sendFromQueue();
      return;
    } else {
      if (Sessions.getSession().isNotAssigned()) {
        lastPossiblyDiscardedMessage = new MessageAndContext<>(message, ctx);
        // Currently the client is working on connecting with central server.
        LOG.debug("Discarded the message" + message + " since the session has not been created yet.");
        ctx.write(Markers.ASSIGN_AID);
        return;
      } else {
        if (!working) {
          lastPossiblyDiscardedMessage = new MessageAndContext<>(message, ctx);
        }
        ctx.writeAndFlush(message);
        return;
      }
    }
  }

  @Override
  public boolean isSharable() {
    return true;
  }

  public static ArrayDeque<MessageAndContext<SequentialMessage>> getMessageQueue() {
    return MESSAGE_QUEUE.clone();
  }

  public static final class MessageAndContext<M> {
    public final M message;
    final ChannelHandlerContext context;

    MessageAndContext(M message, ChannelHandlerContext context) {
      this.message = message;
      this.context = context;
    }
  }
}
