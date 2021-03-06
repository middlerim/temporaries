package com.middlerim.server.channel;

import com.middlerim.server.message.OutboundMessage;

import io.netty.channel.MessageSizeEstimator;
import io.netty.channel.socket.DatagramPacket;

public class ServerMessageSizeEstimator implements MessageSizeEstimator {

  private static final Handle INSTANCE = new Handle();

  @Override
  public io.netty.channel.MessageSizeEstimator.Handle newHandle() {
    return INSTANCE;
  }

  private static class Handle implements io.netty.channel.MessageSizeEstimator.Handle {

    @Override
    public int size(Object msg) {
      if (msg instanceof OutboundMessage) {
        return ((OutboundMessage<?>) msg).message.byteSize();
      } else if (msg instanceof DatagramPacket) {
        return ((DatagramPacket) msg).content().writerIndex();
      }
      throw new UnsupportedOperationException();
    }
  }
}
