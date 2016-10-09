package com.middlerim.client;

import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

import com.middlerim.client.channel.ClientMessageSizeEstimator;
import com.middlerim.client.channel.InboundHandler;
import com.middlerim.client.channel.OutboundHandler;
import com.middlerim.client.channel.PacketToInboundDecoder;
import com.middlerim.client.message.Location;
import com.middlerim.client.message.Markers;
import com.middlerim.client.message.OutboundMessage;
import com.middlerim.client.message.Text;
import com.middlerim.client.session.Sessions;
import com.middlerim.client.view.ViewContext;
import com.middlerim.client.view.ViewEvents;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.AdaptiveRecvByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.nio.NioDatagramChannel;

public class CentralServer {
  private static final String host = "192.168.101.10";
  private static final int port = 1231;
  public static final InetSocketAddress serverAddress = new InetSocketAddress(host, port);

  private static ChannelHandler[] handlers;
  private static ChannelFuture closeFuture;
  private static EventLoopGroup eventGroup;

  private static ViewEvents.Listener<ViewEvents.DestroyEvent> destroyEventListener;
  private static ViewEvents.Listener<ViewEvents.LocationUpdateEvent> locationUpdateEventListener;
  private static ViewEvents.Listener<ViewEvents.SubmitMessageEvent> submitMessageEventListener;

  private static void initializeChannelHandlers(ViewContext viewContext) {
    handlers = new ChannelHandler[]{new PacketToInboundDecoder(viewContext), new InboundHandler(), new OutboundHandler()};
  }

  public static ChannelFuture run(ViewContext viewContext) {
    if (isStarted()) {
      shutdown();
    }
    initializeChannelHandlers(viewContext);
    ChannelFuture bootFuture = createBootstrap().bind(0).addListener(new ChannelFutureListener() {
      @Override
      public void operationComplete(final ChannelFuture f) throws Exception {
        if (!f.isSuccess()) {
          CentralEvents.fireError("E_000", f.cause());
          return;
        }
        initializeEventListeners(f.channel());
        CentralEvents.fireStarted();
      }
    });
    closeFuture = bootFuture.channel().closeFuture();
    return closeFuture;
  }

  private static Bootstrap createBootstrap() {
    eventGroup = new NioEventLoopGroup(1);
    final Bootstrap b = new Bootstrap();
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
    b.option(ChannelOption.SO_SNDBUF, 30);
    b.option(ChannelOption.SO_RCVBUF, 30);
    b.option(ChannelOption.MESSAGE_SIZE_ESTIMATOR, new ClientMessageSizeEstimator());
    b.option(ChannelOption.RCVBUF_ALLOCATOR, new AdaptiveRecvByteBufAllocator(25, 25, 512));
    return b;
  }

  private static void initializeEventListeners(final Channel ch) {
    if (destroyEventListener != null) {
      ViewEvents.removeListener(destroyEventListener);
    }
    destroyEventListener = new ViewEvents.Listener<ViewEvents.DestroyEvent>() {
      @Override
      public void handle(ViewEvents.DestroyEvent event) {
        ch.writeAndFlush(new OutboundMessage<>(Sessions.getSession(), Markers.EXIT))
            .addListener(new ChannelFutureListener() {
              @Override
              public void operationComplete(ChannelFuture f2) throws Exception {
                if (!f2.isSuccess()) {
                  CentralEvents.fireError("E_001", f2.cause());
                }
              }
            });
      }
    };
    ViewEvents.onDestroy(destroyEventListener);

    if (locationUpdateEventListener != null) {
      ViewEvents.removeListener(locationUpdateEventListener);
    }
    locationUpdateEventListener = new ViewEvents.Listener<ViewEvents.LocationUpdateEvent>() {
      @Override
      public void handle(ViewEvents.LocationUpdateEvent event) {
        ch.writeAndFlush(new OutboundMessage<>(Sessions.getSession(), new Location(event.location)))
            .addListener(new ChannelFutureListener() {
              @Override
              public void operationComplete(ChannelFuture f2) throws Exception {
                if (!f2.isSuccess()) {
                  CentralEvents.fireError("E_001", f2.cause());
                }
              }
            });
      }
    };
    ViewEvents.onLocationUpdate(locationUpdateEventListener);

    if (submitMessageEventListener != null) {
      ViewEvents.removeListener(submitMessageEventListener);
    }
    submitMessageEventListener = new ViewEvents.Listener<ViewEvents.SubmitMessageEvent>() {
      @Override
      public void handle(ViewEvents.SubmitMessageEvent event) {
        ch.writeAndFlush(new OutboundMessage<>(Sessions.getSession(), new Text(event.message, event.messageCommand)))
            .addListener(new ChannelFutureListener() {
              @Override
              public void operationComplete(ChannelFuture f2) throws Exception {
                if (!f2.isSuccess()) {
                  CentralEvents.fireError("E_001", f2.cause());
                }
              }
            });
      }
    };
    ViewEvents.onSubmitMessage(submitMessageEventListener);
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
        eventGroup.shutdownGracefully(0, 1000, TimeUnit.SECONDS);
      } catch (Exception e) {
        CentralEvents.fireError("E009", e);
      }
    }
  }

  public static boolean isStarted() {
    return !(closeFuture == null || closeFuture.isCancelled() || closeFuture.isDone());
  }
}
