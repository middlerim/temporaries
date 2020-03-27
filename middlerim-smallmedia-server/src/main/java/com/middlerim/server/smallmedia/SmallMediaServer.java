package com.middlerim.server.smallmedia;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.middlerim.server.Config;
import com.middlerim.server.Server;
import com.middlerim.server.channel.InboundHandler;
import com.middlerim.server.channel.OutboundHandler;
import com.middlerim.server.channel.ServerMessageSizeEstimator;
import com.middlerim.server.message.Markers;
import com.middlerim.server.message.OutboundMessage;
import com.middlerim.server.smallmedia.channel.SmallMediaHandler;
import com.middlerim.server.smallmedia.storage.SessionListener;
import com.middlerim.server.smallmedia.storage.Sessions;
import com.middlerim.session.Session;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.FixedRecvByteBufAllocator;
import io.netty.channel.socket.DatagramChannel;

public class SmallMediaServer extends Server {
  private static final Logger LOG = LoggerFactory.getLogger(SmallMediaServer.class);
  private final ChannelHandler[] sharedHandlers = new ChannelHandler[]{new SmallMediaHandler(), new InboundHandler(), new OutboundHandler()};

  private Channel channelV4;

  private void setSessionExpireListener() {
    Sessions.addListener(new SessionListener() {
      @Override
      public void onRemove(Session session) {
      }
      @Override
      public void onExpire(Session oldSession, Session newSession) {
        channelV4.writeAndFlush(new OutboundMessage<>(newSession, Markers.UPDATE_AID));
      }
    });
  }

  @Override
  protected List<ChannelFuture> listen() {
    EventLoopGroup eventGroup = createEventLoopGroup();
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
    b.option(ChannelOption.SO_SNDBUF, Config.MAX_COMMAND_BYTE * 1000);
    b.option(ChannelOption.SO_RCVBUF, Config.MAX_COMMAND_BYTE * 1000);
    b.option(ChannelOption.MESSAGE_SIZE_ESTIMATOR, new ServerMessageSizeEstimator());
    b.option(ChannelOption.RCVBUF_ALLOCATOR, new FixedRecvByteBufAllocator(Config.MAX_COMMAND_BYTE));

    List<ChannelFuture> futures = new ArrayList<>(2);
    futures.add(b.bind(Config.COMMAND_SERVER_IPV6));

    setSessionExpireListener();
    return futures;
  }

  public static void main(String[] args) {
    new SmallMediaServer().run();
  }
}
