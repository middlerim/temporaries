package com.middlerim.message;

import io.netty.channel.ChannelHandlerContext;

public interface Inbound {

  void processInput(ChannelHandlerContext ctx);
}
