package com.middlerim.server;

import com.middlerim.server.channel.InboundHandler;
import com.middlerim.server.channel.OutboundHandler;
import com.middlerim.server.channel.PacketToInboundDecoder;
import com.middlerim.server.channel.ServerMessageSizeEstimator;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.AdaptiveRecvByteBufAllocator;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.EpollDatagramChannel;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.nio.NioDatagramChannel;

public class UdpServer {

  private final int port;

  public UdpServer(int port) {
    this.port = port;
  }

  public void run() throws Exception {
    final EventLoopGroup eventGroup = createEventLoopGroup();
    try {
      Bootstrap b = new Bootstrap();
      b.group(eventGroup)
          .channel(getChannelClass())
          .handler(new ChannelInitializer<DatagramChannel>() {
            @Override
            public void initChannel(final DatagramChannel ch) throws Exception {
              ch.pipeline().addLast(new PacketToInboundDecoder(), new InboundHandler(), new OutboundHandler());
            }
            @Override
            public boolean isSharable() {
              return true;
            }
          });
      b.option(ChannelOption.SO_REUSEADDR, true);
      b.option(ChannelOption.SO_SNDBUF, 30);
      b.option(ChannelOption.SO_RCVBUF, 30);
      b.option(ChannelOption.MESSAGE_SIZE_ESTIMATOR, new ServerMessageSizeEstimator());
      b.option(ChannelOption.RCVBUF_ALLOCATOR, new AdaptiveRecvByteBufAllocator(25, 25, 512));

      ChannelFuture f = b.bind(port).sync();
      f.channel().closeFuture().sync();
    } finally {
      eventGroup.shutdownGracefully();
    }
  }

  public static void main(String[] args) throws Exception {
    int port;
    if (args.length > 0) {
      port = Integer.parseInt(args[0]);
    } else {
      port = 1231;
    }
    new UdpServer(port).run();
  }

  private EventLoopGroup createEventLoopGroup() {
    return Config.isUnix ? new EpollEventLoopGroup(3) : new NioEventLoopGroup(3);
  }
  private Class<? extends DatagramChannel> getChannelClass() {
    return Config.isUnix
        ? EpollDatagramChannel.class
        : NioDatagramChannel.class;
  }
}
