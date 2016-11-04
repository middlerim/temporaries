package com.middlerim.server;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.middlerim.server.channel.InboundHandler;
import com.middlerim.server.channel.OutboundHandler;
import com.middlerim.server.channel.PacketToInboundDecoder;
import com.middlerim.server.channel.ServerMessageSizeEstimator;
import com.middlerim.server.message.Markers;
import com.middlerim.server.message.OutboundMessage;
import com.middlerim.server.storage.Sessions;
import com.middlerim.session.Session;
import com.middlerim.session.SessionListener;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.FixedRecvByteBufAllocator;
import io.netty.channel.epoll.EpollDatagramChannel;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.util.concurrent.DefaultThreadFactory;

public class UdpServer {
  private static final Logger LOG = LoggerFactory.getLogger(UdpServer.class);
  private static final ChannelHandler[] sharedHandlers = new ChannelHandler[]{new PacketToInboundDecoder(), new InboundHandler(), new OutboundHandler()};
  private static List<ChannelFuture> closeFutures;
  private static EventLoopGroup eventGroup;
  private static ExecutorService embeddedServerService;

  private final InetSocketAddress v4;
  // private final InetSocketAddress v6;
  private final int maximumPacketSize;
  public UdpServer(String hostV4, int portV4, String hostV6, int portV6, int maximumPacketSize) {
    this.v4 = new InetSocketAddress(hostV4, portV4);
    // this.v6 = new InetSocketAddress(hostV6, portV6);
    this.maximumPacketSize = maximumPacketSize;
  }

  public void run() {
    try {
      run0();
      for (ChannelFuture f : closeFutures) {
        f.sync();
      }
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      shutdown();
    }
  }

  private ChannelFuture run0() {
    eventGroup = createEventLoopGroup();
    Bootstrap b = new Bootstrap();
    b.group(eventGroup)
        .channel(getChannelClass())
        .handler(new ChannelInitializer<DatagramChannel>() {
          @Override
          public void initChannel(final DatagramChannel ch) throws Exception {
            ch.pipeline().addLast(sharedHandlers);
          }
          @Override
          public boolean isSharable() {
            return true;
          }
        });
    b.option(ChannelOption.SO_REUSEADDR, true);
    b.option(ChannelOption.SO_SNDBUF, maximumPacketSize * 1000);
    b.option(ChannelOption.SO_RCVBUF, maximumPacketSize * 1000);
    b.option(ChannelOption.MESSAGE_SIZE_ESTIMATOR, new ServerMessageSizeEstimator());
    b.option(ChannelOption.RCVBUF_ALLOCATOR, new FixedRecvByteBufAllocator(maximumPacketSize));

    closeFutures = new ArrayList<>(2);
    ChannelFuture bindFuture = b.bind(v4);
    closeFutures.add(bindFuture.channel().closeFuture());
    // closeFutures.add(b.bind(v6).channel().closeFuture());
    setSessionExpireListener(bindFuture.channel());
    return bindFuture;
  }

  private static void setSessionExpireListener(final Channel channel) {
    Sessions.addListener(new SessionListener() {
      @Override
      public void onRemove(Session session) {
      }
      @Override
      public void onExpire(Session oldSession, Session newSession) {
        channel.writeAndFlush(new OutboundMessage<>(newSession, Markers.UPDATE_AID));
      }
    });
  }

  public static void main(String[] args) {
    new UdpServer(Config.CENTRAL_SERVER_IPV4_HOST, Config.CENTRAL_SERVER_IPV4_PORT, Config.CENTRAL_SERVER_IPV6_HOST, Config.CENTRAL_SERVER_IPV6_PORT, 1280).run();
  }

  public static void runEmbedded() {
    CountDownLatch latch = new CountDownLatch(1);
    new UdpServer(Config.CENTRAL_SERVER_IPV4_HOST, Config.CENTRAL_SERVER_IPV4_PORT, Config.CENTRAL_SERVER_IPV6_HOST, Config.CENTRAL_SERVER_IPV6_PORT, 1280).run0().addListener(new ChannelFutureListener() {
      @Override
      public void operationComplete(ChannelFuture future) throws Exception {
        if (future.cause() != null) {
          future.cause().printStackTrace();
        }
        latch.countDown();
      }
    });
    try {
      latch.await(3, TimeUnit.SECONDS);
    } catch (Exception e) {
      e.printStackTrace();
      shutdown();
    }
  }

  private EventLoopGroup createEventLoopGroup() {
    ThreadFactory tf = new DefaultThreadFactory(Config.INTERNAL_APP_NAME + "-server-background");
    return Config.isUnix ? new EpollEventLoopGroup(3, tf) : new NioEventLoopGroup(3, tf);
  }
  private Class<? extends DatagramChannel> getChannelClass() {
    return Config.isUnix
        ? EpollDatagramChannel.class
        : NioDatagramChannel.class;
  }

  public static void shutdown() {
    if (closeFutures == null) {
      return;
    }
    for (ChannelFuture f : closeFutures) {
      try {
        if (f.channel().isOpen()) {
          f.channel().close();
        }
      } catch (Exception e) {
        LOG.error("Could not shutdown the server(1/2)", e);
      } finally {
        try {
          eventGroup.shutdownGracefully(0, 3, TimeUnit.SECONDS);
        } catch (Exception e) {
          LOG.error("Could not shutdown the server(2/2)", e);
        }
      }
    }
    if (embeddedServerService != null) {
      embeddedServerService.shutdownNow();
    }
  }
}
