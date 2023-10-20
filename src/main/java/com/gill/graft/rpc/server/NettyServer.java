package com.gill.graft.rpc.server;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gill.graft.Node;
import com.gill.graft.common.Utils;
import com.gill.graft.rpc.handler.MetricsHandler;
import com.gill.graft.rpc.handler.SharableChannelHandler;

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
import io.netty.handler.codec.protobuf.ProtobufVarint32FrameDecoder;
import io.netty.handler.ssl.SslContext;
import io.netty.util.concurrent.DefaultThreadFactory;
import io.netty.util.concurrent.UnorderedThreadPoolEventExecutor;

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
			NioEventLoopGroup bossGroup = new NioEventLoopGroup(1, new DefaultThreadFactory("boss-" + node.getId()));
			NioEventLoopGroup workerGroup = new NioEventLoopGroup(0,
					new DefaultThreadFactory("worker-" + node.getId()));
			WorkerSocketChannelHandler workerHandler = new WorkerSocketChannelHandler(null);
			registerMetrics(workerHandler, bossGroup, workerGroup);
			try {
				ServerBootstrap bs = new ServerBootstrap();
				bs.group(bossGroup, workerGroup).channel(NioServerSocketChannel.class)
						.handler(new ServerSocketChannelHandler()).childHandler(workerHandler);
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

	private void registerMetrics(WorkerSocketChannelHandler workerHandler, NioEventLoopGroup boss,
			NioEventLoopGroup worker) {
		workerHandler.metricsHandler.register("boss-threads", boss::executorCount);
		// for (EventExecutor eventExecutor : boss) {
		// if(eventExecutor instanceof NioEventLoop) {
		// NioEventLoop nioEventLoop = (NioEventLoop) eventExecutor;
		// workerHandler.metricsHandler.register(nioEventLoop.,
		// nioEventLoop::pendingTasks);
		// }
		// }
		workerHandler.metricsHandler.register("worker-threads", worker::executorCount);
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

		private final RaftServiceHandler serviceHandler = new RaftServiceHandler(node);

		private final MetricsHandler metricsHandler = new MetricsHandler(node.getId());

		private final UnorderedThreadPoolEventExecutor businessExecutor = new UnorderedThreadPoolEventExecutor(
				Utils.CPU_CORES * 2, new DefaultThreadFactory("business-" + node.getId()));

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
			pl.addLast("protobufFrameEncoder", SharableChannelHandler.PROTOBUF_FRAME_ENCODER);
			pl.addLast("protobufProtocolEncoder", SharableChannelHandler.PROTOBUF_PROTOCOL_ENCODER);

			// in
			pl.addLast("metricsHandler", metricsHandler);
			pl.addLast("protobufFrameDecoder", new ProtobufVarint32FrameDecoder());
			pl.addLast("protobufProtocolDecoder", SharableChannelHandler.PROTOBUF_PROTOCOL_DECODER_REQUEST);
			pl.addLast(businessExecutor, "serviceHandler", serviceHandler);
		}
	}
}
