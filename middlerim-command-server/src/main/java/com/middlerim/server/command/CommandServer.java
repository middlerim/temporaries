package com.middlerim.server.command;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.middlerim.message.Outbound;
import com.middlerim.server.Config;
import com.middlerim.server.Server;
import com.middlerim.server.command.channel.InboundHandler;
import com.middlerim.server.command.channel.OutboundHandler;
import com.middlerim.server.command.channel.PacketToInboundDecoder;
import com.middlerim.server.command.channel.ServerMessageSizeEstimator;
import com.middlerim.server.command.message.Markers;
import com.middlerim.server.command.message.OutboundMessage;
import com.middlerim.server.command.storage.MessageListener;
import com.middlerim.server.command.storage.Messages;
import com.middlerim.server.command.storage.SessionListener;
import com.middlerim.server.command.storage.Sessions;
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

public class CommandServer extends Server {
  private static final Logger LOG = LoggerFactory.getLogger(CommandServer.class);
  private final ChannelHandler[] sharedHandlers = new ChannelHandler[]{new PacketToInboundDecoder(), new InboundHandler(), new OutboundHandler()};

  private final InetSocketAddress v4;
  // private final InetSocketAddress v6;
  private final int maximumPacketSize;
  
  private Channel channelV4;

  public CommandServer() {
    this(Config.CENTRAL_SERVER_IPV4_HOST, Config.CENTRAL_SERVER_IPV4_PORT, Config.CENTRAL_SERVER_IPV6_HOST, Config.CENTRAL_SERVER_IPV6_PORT, Config.MAX_COMMAND_BYTE);
  }

  public CommandServer(String hostV4, int portV4, String hostV6, int portV6, int maximumPacketSize) {
    this.v4 = new InetSocketAddress(hostV4, portV4);
    // this.v6 = new InetSocketAddress(hostV6, portV6);
    this.maximumPacketSize = maximumPacketSize;
  }

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

  private void setMessageListener() {
    Messages.addListener(new MessageListener() {
      @Override
      public void unreached(long userId, Outbound message) {
        Session session = Sessions.getSession(userId);
        LOG.warn("The message is unreached to the client. message: {}, client: {}", message, session != null ? session : "(the session has been expired)");
      }
      @Override
      public void retry(Session recipient, Outbound message) {
        channelV4.writeAndFlush(new OutboundMessage<Outbound>(recipient, message));
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
    b.option(ChannelOption.SO_SNDBUF, maximumPacketSize * 1000);
    b.option(ChannelOption.SO_RCVBUF, maximumPacketSize * 1000);
    b.option(ChannelOption.MESSAGE_SIZE_ESTIMATOR, new ServerMessageSizeEstimator());
    b.option(ChannelOption.RCVBUF_ALLOCATOR, new FixedRecvByteBufAllocator(maximumPacketSize));

    List<ChannelFuture> futures = new ArrayList<>(2);
    futures.add(b.bind(v4));

    // closeFutures.add(b.bind(v6).channel().closeFuture());
    setSessionExpireListener();
    setMessageListener();
    return futures;
  }

  public static void main(String[] args) {
    new CommandServer().run();
  }
}
