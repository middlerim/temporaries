package com.middlerim.message;

import com.middlerim.session.Session;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;

public interface Outbound {

  int byteSize();
  ChannelFuture processOutput(ChannelHandlerContext ctx, Session recipient);
}
