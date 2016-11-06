package com.middlerim.client;

import java.net.InetSocketAddress;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.middlerim.client.channel.ClientMessageSizeEstimator;
import com.middlerim.client.channel.InboundHandler;
import com.middlerim.client.channel.OutboundHandler;
import com.middlerim.client.channel.OutboundSynchronizer;
import com.middlerim.client.channel.PacketToInboundDecoder;
import com.middlerim.client.message.Location;
import com.middlerim.client.message.Markers;
import com.middlerim.client.message.Text;
import com.middlerim.client.session.Sessions;
import com.middlerim.client.view.ViewContext;
import com.middlerim.client.view.ViewEvents;
import com.middlerim.location.Point;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.FixedRecvByteBufAllocator;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.util.concurrent.DefaultThreadFactory;

public class CentralServer {
  private static final Logger LOG = LoggerFactory.getLogger(Config.INTERNAL_APP_NAME);
  public static final InetSocketAddress serverIPv4Address = new InetSocketAddress(Config.CENTRAL_SERVER_IPV4_HOST, Config.CENTRAL_SERVER_IPV4_PORT);

  private static ChannelHandler[] handlers;
  private static ChannelFuture closeFuture;
  private static EventLoopGroup eventGroup;

  private static ViewEvents.Listener<ViewEvents.DestroyEvent> destroyEventListener;
  private static ViewEvents.Listener<ViewEvents.LocationUpdateEvent> locationUpdateEventListener;
  private static ViewEvents.Listener<ViewEvents.SubmitMessageEvent> submitMessageEventListener;

  private static Timer timer;
  private static long lastTouchMillis = -1;
  private static Point lastLocation;

  private static class KeepAliveTask extends TimerTask {
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
      LOG.debug("Sent keep-alive signal to the central server.");
    }
  };
  private static void runKeepingSessionAliveTimer(Channel channel) {
    if (Config.KEEP_ALIVE_ENABLED) {
      timer = new Timer(Config.INTERNAL_APP_NAME + "-keepAlive", true);
      timer.schedule(new KeepAliveTask(channel), Config.SESSION_TIMEOUT_MILLIS, Config.SESSION_TIMEOUT_MILLIS - 10_000);
    } else {
      LOG.warn("Keep alive is disabled.");
    }
  }

  private static void initializeChannelHandlers(ViewContext viewContext) {
    handlers = new ChannelHandler[]{
        // In
        new PacketToInboundDecoder(viewContext), new InboundHandler(),
        // Out
        new OutboundHandler(), new OutboundSynchronizer()};
  }

  public static ChannelFuture run(ViewContext viewContext) {
    if (isStarted()) {
      shutdown();
    }
    initializeChannelHandlers(viewContext);

    eventGroup = new NioEventLoopGroup(1, new DefaultThreadFactory(Config.INTERNAL_APP_NAME + "-client-background"));
    Bootstrap b = new Bootstrap();
    b.group(eventGroup)
        .channel(NioDatagramChannel.class)
        .handler(new ChannelInitializer<DatagramChannel>() {
          @Override
          public void initChannel(final DatagramChannel ch) throws Exception {
            ch.pipeline().addLast(handlers);
          }
          @Override
          public boolean isSharable() {
            return true;
          }
        });
    b.option(ChannelOption.SO_REUSEADDR, true);
    b.option(ChannelOption.SO_SNDBUF, Config.MAX_PACKET_SIZE * 3);
    b.option(ChannelOption.SO_RCVBUF, Config.MAX_PACKET_SIZE * 3);
    b.option(ChannelOption.MESSAGE_SIZE_ESTIMATOR, new ClientMessageSizeEstimator());
    b.option(ChannelOption.RCVBUF_ALLOCATOR, new FixedRecvByteBufAllocator(Config.MAX_PACKET_SIZE));

    ChannelFuture bootFuture = b.bind(0).addListener(new ChannelFutureListener() {
      @Override
      public void operationComplete(final ChannelFuture f) throws Exception {
        if (!f.isSuccess()) {
          CentralEvents.fireError("E_000", f.cause());
          return;
        }
        if (Sessions.getSession() == null) {
          Sessions.setAnonymous();
          f.channel().writeAndFlush(Markers.ASSIGN_AID);
        }
        initializeEventListeners(f.channel());
        CentralEvents.fireStarted();
        runKeepingSessionAliveTimer(f.channel());
      }
    });
    closeFuture = bootFuture.channel().closeFuture();
    return bootFuture;
  }

  private static void initializeEventListeners(final Channel ch) {
    if (destroyEventListener != null) {
      ViewEvents.removeListener("CentralServer.destroyEventListener");
    }
    destroyEventListener = new ViewEvents.Listener<ViewEvents.DestroyEvent>() {
      @Override
      public void handle(ViewEvents.DestroyEvent event) {
        ch.writeAndFlush(Markers.EXIT)
            .addListener(new ChannelFutureListener() {
              @Override
              public void operationComplete(ChannelFuture f2) throws Exception {
                if (!f2.isSuccess()) {
                  CentralEvents.fireError("E_001", f2.cause());
                }
                CentralServer.shutdown();
              }
            });
      }
    };
    ViewEvents.onDestroy("CentralServer.destroyEventListener", destroyEventListener);

    if (locationUpdateEventListener != null) {
      ViewEvents.removeListener("CentralServer.locationUpdateEventListener");
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
        ch.writeAndFlush(new Location(location));
        lastTouchMillis = System.currentTimeMillis();
      }
    };
    ViewEvents.onLocationUpdate("CentralServer.locationUpdateEventListener", locationUpdateEventListener);

    if (submitMessageEventListener != null) {
      ViewEvents.removeListener("CentralServer.submitMessageEventListener");
    }
    submitMessageEventListener = new ViewEvents.Listener<ViewEvents.SubmitMessageEvent>() {
      @Override
      public void handle(ViewEvents.SubmitMessageEvent event) {
        ch.writeAndFlush(new Text.Out(event.tag, event.displayName, event.messageCommand, event.message));
        lastTouchMillis = System.currentTimeMillis();
      }
    };
    ViewEvents.onSubmitMessage("CentralServer.submitMessageEventListener", submitMessageEventListener);
  }

  public static void shutdown() {
    if (!isStarted()) {
      return;
    }
    try {
      closeFuture.channel().close();
    } catch (Exception e) {
      CentralEvents.fireError("E009", e);
    } finally {
      try {
        eventGroup.shutdownGracefully(0, 10, TimeUnit.SECONDS);
      } catch (Exception e) {
        CentralEvents.fireError("E009", e);
      }
    }
  }

  public static boolean isStarted() {
    return !(closeFuture == null || closeFuture.isCancelled() || closeFuture.isDone());
  }
}
