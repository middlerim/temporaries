package com.middlerim.client.central;

import java.net.InetSocketAddress;
import java.util.ArrayDeque;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.middlerim.client.Config;
import com.middlerim.client.channel.InboundHandler;
import com.middlerim.client.channel.OutboundHandler;
import com.middlerim.client.channel.OutboundSynchronizer;
import com.middlerim.client.channel.OutboundSynchronizer.MessageAndContext;
import com.middlerim.client.channel.PacketToInboundDecoder;
import com.middlerim.client.message.Location;
import com.middlerim.client.message.Markers;
import com.middlerim.client.message.Text;
import com.middlerim.client.view.ViewContext;
import com.middlerim.client.view.ViewEvents;
import com.middlerim.location.Point;
import com.middlerim.message.Outbound;
import com.middlerim.message.SequentialMessage;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelConfig;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.FixedRecvByteBufAllocator;
import io.netty.channel.MessageSizeEstimator;
import io.netty.channel.pool.ChannelPoolHandler;
import io.netty.channel.pool.SimpleChannelPool;
import io.netty.channel.socket.DatagramPacket;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.FutureListener;

public final class CommandServer implements ChannelPoolHandler {
  private static final Logger LOG = LoggerFactory.getLogger(Config.INTERNAL_APP_NAME);

  private static SimpleChannelPool pool;

  private static ViewContext viewContext;

  private static ViewEvents.Listener<ViewEvents.DestroyEvent> destroyEventListener;
  private static ViewEvents.Listener<ViewEvents.LocationUpdateEvent> locationUpdateEventListener;
  private static ViewEvents.Listener<ViewEvents.SubmitMessageEvent> submitMessageEventListener;

  private static long lastTouchMillis = -1;
  private static Point lastLocation;

  private static Channel channel;
  private static OutboundSynchronizer outboundSynchronizer;

  public static Channel channel() {
    return channel;
  }

  @Override
  public void channelCreated(Channel ch) throws Exception {
    configureChannel(ch);
    initializeChannelHandlers(ch);
  }

  private void configureChannel(Channel ch) {
    ChannelConfig config = ch.config();
    config.setRecvByteBufAllocator(new FixedRecvByteBufAllocator(Config.MAX_COMMAND_BYTE));
    config.setMessageSizeEstimator(new ClientMessageSizeEstimator());
  }

  private static void initializeChannelHandlers(Channel ch) {
    outboundSynchronizer = new OutboundSynchronizer();
    ChannelHandler[] handlers = new ChannelHandler[]{
        // In
        new PacketToInboundDecoder(viewContext), new InboundHandler(),
        // Out
        new OutboundHandler(), outboundSynchronizer};

    ch.pipeline().addLast(handlers);
  }

  @Override
  public void channelAcquired(Channel ch) throws Exception {

  }

  @Override
  public void channelReleased(Channel ch) throws Exception {
  }

  public static ChannelFuture listen(Bootstrap b) {
    pool = new SimpleChannelPool(b.remoteAddress(Config.COMMAND_SERVER), new CommandServer());

    // Keep 1 channel for listening global events.
    Future<?> future = pool.acquire().addListener(new FutureListener<Channel>() {
      @Override
      public void operationComplete(Future<Channel> f) throws Exception {
        if (f.cause() == null) {
          channel = f.getNow();
          initializeEventListeners();
          runKeepingSessionAliveTimer();
        } else {
          CentralEvents.fireError("E_001", f.cause());
          LOG.error("Channel initialing error", f.cause());
        }
      }
    });
    try {
      boolean result = future.await(1000);
      if (!result) {
        throw new RuntimeException("Could not find the command server: " + Config.COMMAND_SERVER);
      }
      if (!future.isSuccess()) {
        throw new RuntimeException("Could not find the command server: " + Config.COMMAND_SERVER, future.cause());
      }
    } catch (InterruptedException e) {
      throw new RuntimeException("Could not find the command server: " + Config.COMMAND_SERVER, e);
    }
    return channel.bind(new InetSocketAddress(0));
  }

  private static void initializeEventListeners() {
    if (destroyEventListener != null) {
      ViewEvents.removeListener("CommandServer.destroyEventListener");
    }
    destroyEventListener = new ViewEvents.Listener<ViewEvents.DestroyEvent>() {
      @Override
      public void handle(ViewEvents.DestroyEvent event) {
        channel.writeAndFlush(Markers.EXIT)
            .addListener(new ChannelFutureListener() {
              @Override
              public void operationComplete(ChannelFuture f2) throws Exception {
                if (!f2.isSuccess()) {
                  CentralEvents.fireError("E_001", f2.cause());
                  LOG.error("Could not exit.", f2.cause());
                }
                CentralServer.shutdown();
              }
            });
      }
    };
    ViewEvents.onDestroy("CommandServer.destroyEventListener", destroyEventListener);

    if (locationUpdateEventListener != null) {
      ViewEvents.removeListener("CommandServer.locationUpdateEventListener");
    }
    locationUpdateEventListener = new ViewEvents.Listener<ViewEvents.LocationUpdateEvent>() {
      private boolean isSameAsLastLocation(Point location) {
        if (lastLocation == null) {
          return false;
        }
        return lastLocation.distanceMeter(location) <= Config.ALLOWABLE_MARGIN_LOCATION_METER;
      }
      @Override
      public void handle(ViewEvents.LocationUpdateEvent event) {
        Point location = event.location.toPoint();
        if (isSameAsLastLocation(location)) {
          return;
        }
        lastLocation = location;
        channel.writeAndFlush(new Location(location));
        lastTouchMillis = System.currentTimeMillis();
      }
    };
    ViewEvents.onLocationUpdate("CommandServer.locationUpdateEventListener", locationUpdateEventListener);

    if (submitMessageEventListener != null) {
      ViewEvents.removeListener("CommandServer.submitMessageEventListener");
    }
    submitMessageEventListener = new ViewEvents.Listener<ViewEvents.SubmitMessageEvent>() {
      @Override
      public void handle(ViewEvents.SubmitMessageEvent event) {
        channel.writeAndFlush(new Text.Out(event.tag, event.displayName, event.messageCommand, event.message));
        lastTouchMillis = System.currentTimeMillis();
      }
    };
    ViewEvents.onSubmitMessage("CommandServer.submitMessageEventListener", submitMessageEventListener);
  }

  private static class KeepAliveTask implements Runnable {
    private Channel channel;
    public KeepAliveTask(Channel channel) {
      this.channel = channel;
    }

    @Override
    public void run() {
      if (lastLocation == null || lastTouchMillis + (Config.SESSION_TIMEOUT_MILLIS / 2) > System.currentTimeMillis()) {
        return;
      }
      channel.writeAndFlush(new Location(lastLocation));
      lastTouchMillis = System.currentTimeMillis();
      LOG.debug("Sent keep-alive signal to the command server.");
    }
  };

  private static void runKeepingSessionAliveTimer() {
    if (Config.KEEP_ALIVE_ENABLED) {
      channel.eventLoop().scheduleAtFixedRate(new KeepAliveTask(channel), Config.SESSION_TIMEOUT_MILLIS, Config.SESSION_TIMEOUT_MILLIS - 10_000, TimeUnit.MILLISECONDS);
    } else {
      LOG.warn("Keep alive is disabled.");
    }
  }

  public static ArrayDeque<MessageAndContext<SequentialMessage>> getMessageQueue() {
    if (outboundSynchronizer == null) {
      return new ArrayDeque<>(0);
    }
    return outboundSynchronizer.getMessageQueue();
  }

  private static class ClientMessageSizeEstimator implements MessageSizeEstimator {

    private static final Handle INSTANCE = new Handle();

    @Override
    public io.netty.channel.MessageSizeEstimator.Handle newHandle() {
      return INSTANCE;
    }

    private static class Handle implements io.netty.channel.MessageSizeEstimator.Handle {

      @Override
      public int size(Object msg) {
        if (msg instanceof Outbound) {
          return ((Outbound) msg).byteSize();
        } else if (msg instanceof DatagramPacket) {
          return ((DatagramPacket) msg).content().writerIndex();
        }
        throw new UnsupportedOperationException();
      }
    }
  }
}
