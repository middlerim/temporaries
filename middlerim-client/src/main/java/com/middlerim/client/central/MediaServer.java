package com.middlerim.client.central;

import java.net.InetSocketAddress;

import com.middlerim.client.Config;

import io.netty.channel.Channel;
import io.netty.channel.ChannelConfig;
import io.netty.channel.FixedRecvByteBufAllocator;
import io.netty.channel.pool.ChannelPoolHandler;

public final class MediaServer implements ChannelPoolHandler {

  public static final InetSocketAddress serverIPv4Address = new InetSocketAddress(Config.CENTRAL_SERVER_IPV4_HOST, Config.CENTRAL_SERVER_IPV4_PORT);

  @Override
  public void channelCreated(Channel ch) throws Exception {
    configureChannel(ch);
  }

  private void configureChannel(Channel ch) {
    ChannelConfig config = ch.config();
    config.setRecvByteBufAllocator(new FixedRecvByteBufAllocator(Config.MAX_MEDIA_MESSAGE_BYTE));
  }

  @Override
  public void channelAcquired(Channel ch) throws Exception {

  }

  @Override
  public void channelReleased(Channel ch) throws Exception {

  }
}
