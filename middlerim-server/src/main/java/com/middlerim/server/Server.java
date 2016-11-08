package com.middlerim.server;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.EventLoop;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.EpollDatagramChannel;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.util.concurrent.DefaultThreadFactory;

public abstract class Server {
  private static final Logger LOG = LoggerFactory.getLogger(Server.class);

  private List<ChannelFuture> channelFutures;

  public void run() {
    try {
      channelFutures = listen();
      channelFutures.stream().map((f) -> f.channel().closeFuture()).forEach(f -> {
        try {
          f.sync();
        } catch (InterruptedException e) {
          // Ignore
        }
      });
    } finally {
      shutdown();
    }
  }

  protected abstract List<ChannelFuture> listen();

  public void runEmbedded() {
    channelFutures = listen();
    CountDownLatch latch = new CountDownLatch(channelFutures.size());

    channelFutures.forEach((f) -> {
      f.addListener(new ChannelFutureListener() {
        @Override
        public void operationComplete(ChannelFuture future) throws Exception {
          if (future.cause() != null) {
            LOG.error("Unexpected exception", future.cause());
          }
          latch.countDown();
        }
      });
    });
    try {
      latch.await(3, TimeUnit.SECONDS);
    } catch (Exception e) {
      LOG.error("Timeout", e);
      shutdown();
    }
  }

  protected EventLoopGroup createEventLoopGroup() {
    ThreadFactory tf = new DefaultThreadFactory(Config.INTERNAL_APP_NAME + "-server-background");
    return Config.isUnix ? new EpollEventLoopGroup(3, tf) : new NioEventLoopGroup(3, tf);
  }
  protected Class<? extends DatagramChannel> getChannelClass() {
    return Config.isUnix
        ? EpollDatagramChannel.class
        : NioDatagramChannel.class;
  }

  public void shutdown() {
    if (channelFutures == null) {
      return;
    }
    EventLoop el = channelFutures.get(0).channel().eventLoop();
    channelFutures.stream().map((f) -> f.channel().closeFuture()).forEach((f -> {
      try {
        if (f.channel().isOpen()) {
          f.channel().close();
        }
      } catch (Exception e) {
        LOG.error("Could not shutdown the server: {}", f.channel().localAddress(), e);
      }
    }));
    try {
      el.shutdownGracefully(0, 3, TimeUnit.SECONDS);
    } catch (Exception e) {
      LOG.error("Could not shutdown the event loop on the server.", e);
    }
  }
}
