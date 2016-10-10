package com.middlerim.admin;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;

public class Server {
  public static void main(String[] args) throws Exception {
    ServerBootstrap b = new ServerBootstrap();

    NioEventLoopGroup bgroup = new NioEventLoopGroup();
    NioEventLoopGroup wgroup = new NioEventLoopGroup();

    try {
      b.group(bgroup, wgroup)
          .channel(NioServerSocketChannel.class)
          .childHandler(new SampleChannelInitializer());

      Channel ch = b.bind(8080).sync().channel();
      ch.closeFuture().sync();

    } finally {
      bgroup.shutdownGracefully();
      wgroup.shutdownGracefully();
    }
  }
}

class Handler extends SimpleChannelInboundHandler<TextWebSocketFrame> {
  @Override
  public void channelRead0(ChannelHandlerContext ctx, TextWebSocketFrame frame) {
    ctx.channel().writeAndFlush(new TextWebSocketFrame("echo : " + frame.text()));
  }
}

class SampleChannelInitializer extends ChannelInitializer<SocketChannel> {

  @Override
  public void initChannel(SocketChannel ch) {
    ch.pipeline().addLast(
        new HttpServerCodec(),
        new HttpObjectAggregator(65536),
        new WebSocketServerProtocolHandler("/sample"),
        new Handler());
  }
}
