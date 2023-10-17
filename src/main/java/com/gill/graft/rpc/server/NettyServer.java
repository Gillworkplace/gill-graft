package com.gill.graft.rpc.server;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gill.graft.Node;
import com.gill.graft.proto.Raft;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.protobuf.ProtobufDecoder;
import io.netty.handler.codec.protobuf.ProtobufEncoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32FrameDecoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32LengthFieldPrepender;
import io.netty.handler.ssl.SslContext;

/**
 * NettyServer
 *
 * @author gill
 * @version 2023/10/11
 **/
public class NettyServer {

	private static final Logger log = LoggerFactory.getLogger(NettyServer.class);

	private final ExecutorService executor = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS,
			new LinkedBlockingQueue<>(1), r -> new Thread(r, "netty-server"));

	private volatile boolean running = false;

	private final Node node;

	public NettyServer(Node node) {
		this.node = node;
	}

	/**
	 * 启动netty server
	 */
	public synchronized void start(int port) {
		if (running) {
			return;
		}
		running = true;
		CountDownLatch latch = new CountDownLatch(1);
		executor.execute(() -> {
			NioEventLoopGroup bossGroup = new NioEventLoopGroup(1);
			NioEventLoopGroup workerGroup = new NioEventLoopGroup();
			try {
				ServerBootstrap bs = new ServerBootstrap();
				bs.group(bossGroup, workerGroup).channel(NioServerSocketChannel.class)
						.handler(new ServerSocketChannelHandler()).childHandler(new WorkerSocketChannelHandler(null));
				ChannelFuture channelFuture = bs.bind(port).sync();
				log.info("netty server initialized");
				latch.countDown();
				channelFuture.channel().closeFuture().sync();
				log.info("netty server closed");
			} catch (InterruptedException e) {
				log.error("netty server interrupted, e: {}", e.getMessage());
			} finally {
				bossGroup.shutdownGracefully();
				workerGroup.shutdownGracefully();
			}
		});
		try {
			if (!latch.await(30, TimeUnit.SECONDS)) {
				log.error("start netty server failed.");
			}
		} catch (InterruptedException ignored) {
		}
	}

	static class ServerSocketChannelHandler extends ChannelInboundHandlerAdapter {

		private static final Logger log = LoggerFactory.getLogger(ServerSocketChannelHandler.class);

		@Override
		public void channelActive(ChannelHandlerContext ctx) {
			log.info("netty server active.");
			ctx.fireChannelActive();
		}

		@Override
		public void channelRead(ChannelHandlerContext ctx, Object msg) {
			if (msg instanceof NioSocketChannel) {
				NioSocketChannel sc = (NioSocketChannel) msg;
				log.info("accept remote ip: {}", sc.remoteAddress());
			} else {
				log.info("accept {}", msg);
			}
			ctx.fireChannelRead(msg);
		}

		@Override
		public void channelInactive(ChannelHandlerContext ctx) {
			log.info("netty server inactive.");
			ctx.fireChannelInactive();
		}
	}

	class WorkerSocketChannelHandler extends ChannelInitializer<SocketChannel> {

		private final SslContext sslCtx;

		public WorkerSocketChannelHandler(SslContext sslCtx) {
			this.sslCtx = sslCtx;
		}

		@Override
		protected void initChannel(SocketChannel sc) {
			ChannelPipeline pl = sc.pipeline();
			if (sslCtx != null) {
				pl.addLast("SSL", sslCtx.newHandler(sc.alloc()));
			}

			// out
			pl.addLast(new ProtobufVarint32LengthFieldPrepender());
			pl.addLast(new ProtobufEncoder());

			// in
			pl.addLast(new ProtobufVarint32FrameDecoder());
			pl.addLast(new ProtobufDecoder(Raft.Request.getDefaultInstance()));
			pl.addLast(new RaftServiceHandler(node));
		}
	}
}
