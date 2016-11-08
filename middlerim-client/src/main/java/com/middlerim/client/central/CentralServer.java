package com.middlerim.client.central;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.middlerim.client.Config;
import com.middlerim.client.message.Markers;
import com.middlerim.client.session.Sessions;
import com.middlerim.client.view.ViewContext;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.util.concurrent.DefaultThreadFactory;

public class CentralServer {
  private static List<ChannelFuture> closeFutures = new ArrayList<>(2);
  private static EventLoopGroup eventGroup;

  public static ChannelFuture run(ViewContext viewContext) {
    if (isStarted()) {
      shutdown();
    }

    eventGroup = new NioEventLoopGroup(1, new DefaultThreadFactory(Config.INTERNAL_APP_NAME + "-client-background"));
    final Bootstrap b = new Bootstrap();
    b.group(eventGroup)
        .channel(NioDatagramChannel.class)
        .option(ChannelOption.SO_REUSEADDR, true)
        .option(ChannelOption.SO_SNDBUF, Config.MAX_COMMAND_BYTE * 100 + Config.MAX_MEDIA_MESSAGE_BYTE * 2)
        .option(ChannelOption.SO_RCVBUF, Config.MAX_COMMAND_BYTE * 100 + Config.MAX_MEDIA_MESSAGE_BYTE * 2);

    ChannelFuture bootFuture = CommandServer.listen(b);
    if (Sessions.getSession() == null) {
      Sessions.setAnonymous();
      CommandServer.channel().writeAndFlush(Markers.ASSIGN_AID);
    }
    CentralEvents.fireStarted();
    closeFutures.add(bootFuture.channel().closeFuture());
    return bootFuture;
  }

  public static boolean isStarted() {
    for (ChannelFuture f : closeFutures) {
      if (isStarted(f)) {
        return true;
      }
    }
    return false;
  }

  public static void shutdown() {
    for (ChannelFuture f : closeFutures) {
      shutdown(f);
    }
    try {
      eventGroup.shutdownGracefully(0, 10, TimeUnit.SECONDS);
    } catch (Exception e) {
      CentralEvents.fireError("E009", e);
    }
  }

  private static boolean isStarted(ChannelFuture closeFuture) {
    return !(closeFuture == null || closeFuture.isCancelled() || closeFuture.isDone());
  }

  private static void shutdown(ChannelFuture closeFuture) {
    if (!isStarted(closeFuture)) {
      return;
    }
    try {
      closeFuture.channel().close();
    } catch (Exception e) {
      CentralEvents.fireError("E009", e);
    }
  }
}
