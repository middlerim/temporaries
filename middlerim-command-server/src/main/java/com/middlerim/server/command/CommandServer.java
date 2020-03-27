package com.middlerim.server.command;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.middlerim.message.Outbound;
import com.middlerim.server.Server;
import com.middlerim.server.channel.InboundHandler;
import com.middlerim.server.channel.OutboundHandler;
import com.middlerim.server.channel.ServerMessageSizeEstimator;
import com.middlerim.server.command.channel.CommandHandler;
import com.middlerim.server.command.storage.MessageListener;
import com.middlerim.server.command.storage.Messages;
import com.middlerim.server.message.Markers;
import com.middlerim.server.message.OutboundMessage;
import com.middlerim.server.storage.SessionListener;
import com.middlerim.server.storage.Sessions;
import com.middlerim.session.Session;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.FixedRecvByteBufAllocator;
import io.netty.channel.socket.DatagramChannel;

public class CommandServer extends Server {
  private static final Logger LOG = LoggerFactory.getLogger(CommandServer.class);
  private final ChannelHandler[] sharedHandlers = new ChannelHandler[]{new CommandHandler(), new InboundHandler(), new OutboundHandler()};

  private Channel channelV4;
  private Channel channelV6;
  private Channel channel(InetSocketAddress address) {
    if (address == Config.COMMAND_SERVER_IPV4) {
      return channelV4;
    } else if (address == Config.COMMAND_SERVER_IPV6) {
      return channelV6;
    } else {
      throw new IllegalStateException("Unknown address: " + address);
    }
  }

  private void setSessionExpireListener() {
    Sessions.addListener(new SessionListener() {
      @Override
      public void onRemove(Session session) {
      }
      @Override
      public void onExpire(Session oldSession, Session newSession) {
        channel(newSession.address).writeAndFlush(new OutboundMessage<>(newSession, Markers.UPDATE_AID));
      }
    });
  }

  private void setMessageListener() {
    Messages.addListener(new MessageListener() {
      @Override
      public void unreached(long userId, Outbound message) {
        Session session = Sessions.getSession(userId);
        LOG.warn("The message is unreached to the client. message: {}, client: {}", message, session != null ? session : "(the session has been expired)");
      }
      @Override
      public void retry(Session recipient, Outbound message) {
        channel(recipient.address).writeAndFlush(new OutboundMessage<Outbound>(recipient, message));
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
    futures.add(b.bind(Config.COMMAND_SERVER_IPV4).addListener(new ChannelFutureListener() {

      @Override
      public void operationComplete(ChannelFuture future) throws Exception {
        channelV4 = future.channel();
      }
    }));
    futures.add(b.bind(Config.COMMAND_SERVER_IPV6).addListener(new ChannelFutureListener() {

      @Override
      public void operationComplete(ChannelFuture future) throws Exception {
        channelV6 = future.channel();
      }
    }));

    setSessionExpireListener();
    setMessageListener();
    return futures;
  }

  public static void main(String[] args) {
    new CommandServer().run();
  }
}
