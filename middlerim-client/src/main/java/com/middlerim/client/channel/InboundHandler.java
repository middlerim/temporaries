package com.middlerim.client.channel;

import java.util.List;

import com.middlerim.message.Inbound;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;

public class InboundHandler extends MessageToMessageDecoder<Inbound> {

  @Override
  protected void decode(ChannelHandlerContext ctx, Inbound input, List<Object> out) throws Exception {
    input.processInput(ctx);
  }

  @Override
  public boolean isSharable() {
    return true;
  }
}
