package com.middlerim.client.central;

import java.net.InetSocketAddress;

import com.middlerim.client.Config;
import com.middlerim.client.view.ViewContext;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelConfig;
import io.netty.channel.ChannelFuture;
import io.netty.channel.FixedRecvByteBufAllocator;
import io.netty.channel.pool.ChannelPoolHandler;
import io.netty.channel.pool.SimpleChannelPool;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.FutureListener;

public final class SmallMediaServer implements ChannelPoolHandler {

  private static SimpleChannelPool pool;

  private static ViewContext viewContext;
  private static Channel channel;

  @Override
  public void channelCreated(Channel ch) throws Exception {
    configureChannel(ch);
  }

  private void configureChannel(Channel ch) {
    ChannelConfig config = ch.config();
    config.setRecvByteBufAllocator(new FixedRecvByteBufAllocator(Config.MAX_SMALLMEDIA_BYTE));
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
        if (f.isSuccess()) {
          channel = f.getNow();
        } else {
          throw new RuntimeException(f.cause());
        }
      }
    });
    try {
      boolean result = future.await(1000);
      if (!result) {
        throw new RuntimeException("Could not find the small media server: " + Config.COMMAND_SERVER);
      }
    } catch (InterruptedException e) {
      throw new RuntimeException("Could not find the small media server: " + Config.COMMAND_SERVER, e);
    }
    return channel.bind(new InetSocketAddress(0));
  }

}
