package com.gill.graft.rpc.server;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gill.graft.Node;
import com.gill.graft.apis.Server;
import com.gill.graft.common.Utils;
import com.gill.graft.config.RaftConfig;
import com.gill.graft.rpc.MetricsRegistry;
import com.gill.graft.rpc.ServiceRegistry;
import com.gill.graft.rpc.handler.AuthHandler;
import com.gill.graft.rpc.handler.GlobalSocketChannelStatisticsHandler;
import com.gill.graft.rpc.handler.ServerIdleStateHandler;
import com.gill.graft.rpc.handler.SharableChannelHandler;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.protobuf.ProtobufVarint32FrameDecoder;
import io.netty.handler.ssl.SslContext;
import io.netty.util.Version;
import io.netty.util.concurrent.DefaultThreadFactory;
import io.netty.util.concurrent.UnorderedThreadPoolEventExecutor;

/**
 * NettyServer
 *
 * @author gill
 * @version 2023/10/11
 **/
public class NettyServer implements Server {

	private static final Logger log = LoggerFactory.getLogger(NettyServer.class);

	private final ExecutorService executor = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS,
			new LinkedBlockingQueue<>(1), r -> new Thread(r, "netty-server"));

	private final AtomicBoolean ready = new AtomicBoolean(false);

	private volatile boolean running = false;

	private volatile Channel c;

	private final int nodeId;

	private final Supplier<Integer> port;

	private final Supplier<RaftConfig.NettyConfig> nettyConfig;

	private final Supplier<RaftConfig.AuthConfig> authConfig;

	private final MetricsRegistry metricsRegistry;

	private final ServiceRegistry serviceRegistry;

	public NettyServer(Node node) {
		this.nodeId = node.getId();
		this.port = () -> node.getConfig().getPort();
		this.nettyConfig = () -> node.getConfig().getNettyConfig();
		this.authConfig = () -> node.getConfig().getAuthConfig();
		this.metricsRegistry = node.getMetricsRegistry();
		this.serviceRegistry = node.getServiceRegistry();
	}

	/**
	 * netty server是否已启动
	 * 
	 * @return 启动
	 */
	public boolean isReady() {
		return ready.get();
	}

	private void ready() {
		ready.set(true);
	}

	private void unready() {
		ready.set(false);
	}

	/**
	 * 启动netty server
	 */
	public synchronized void start() {
		if (running) {
			return;
		}
		running = true;
		CountDownLatch latch = new CountDownLatch(1);
		executor.execute(() -> {
			NioEventLoopGroup bossGroup = new NioEventLoopGroup(1, new DefaultThreadFactory("boss-" + nodeId));
			NioEventLoopGroup workerGroup = new NioEventLoopGroup(0, new DefaultThreadFactory("worker-" + nodeId));
			registerMetrics(bossGroup, workerGroup);
			WorkerSocketChannelHandler workerHandler = new WorkerSocketChannelHandler(null);
			try {
				ServerBootstrap bs = new ServerBootstrap();
				bs.group(bossGroup, workerGroup).handler(new ServerSocketChannelHandler()).childHandler(workerHandler)
						.option(ChannelOption.SO_BACKLOG, 1024).option(ChannelOption.SO_REUSEADDR, Boolean.TRUE)
						.childOption(ChannelOption.TCP_NODELAY, Boolean.TRUE)
						.childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);
				selectServerChannelType(bs);
				ChannelFuture channelFuture = bs.bind(port.get()).sync();
				ready();
				log.info("netty server initialized nodeId: {} bind to {}", nodeId, port);
				logNettyVersion();
				c = channelFuture.channel();
				latch.countDown();
				c.closeFuture().sync();
				log.info("netty server closed");
			} catch (InterruptedException e) {
				log.error("netty server interrupted, e: {}", e.getMessage());
			} finally {
				unready();
				bossGroup.shutdownGracefully().syncUninterruptibly();
				workerGroup.shutdownGracefully().syncUninterruptibly();
			}
		});
		try {
			if (!latch.await(30, TimeUnit.SECONDS)) {
				log.error("start netty server failed.");
			}
		} catch (InterruptedException ignored) {
		}
	}

	private void logNettyVersion() {
		for (Map.Entry<String, Version> entry : Version.identify().entrySet()) {
			log.info("{}: {}", entry.getKey(), entry.getValue().toString());
		}
	}

	public synchronized void stop() {
		log.info("netty server is stopping.");
		running = false;
		if (c != null && c.isOpen()) {
			try {
				c.close().sync();
				c = null;
			} catch (InterruptedException e) {
				log.error("occur interrupted when netty server is closing, e: {}", e.getMessage());
			}
		}
	}

	private static void selectServerChannelType(ServerBootstrap bs) {
		if (Utils.isLinux()) {
			bs.channel(EpollServerSocketChannel.class);
		} else {
			bs.channel(NioServerSocketChannel.class);
		}
	}

	private void registerMetrics(NioEventLoopGroup boss, NioEventLoopGroup worker) {
		metricsRegistry.register("boss-threads", boss::executorCount);
		// for (EventExecutor eventExecutor : boss) {
		// if(eventExecutor instanceof NioEventLoop) {
		// NioEventLoop nioEventLoop = (NioEventLoop) eventExecutor;
		// metricsRegistry.register(nioEventLoop.,
		// nioEventLoop::pendingTasks);
		// }
		// }
		metricsRegistry.register("worker-threads", worker::executorCount);
	}

	@ChannelHandler.Sharable
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

		private final RaftServiceHandler serviceHandler = new RaftServiceHandler(serviceRegistry);

		private final GlobalSocketChannelStatisticsHandler globalSocketChannelStatisticsHandler;

		private final AuthHandler authHandler = new AuthHandler(nodeId, authConfig);

		private final UnorderedThreadPoolEventExecutor businessExecutor = new UnorderedThreadPoolEventExecutor(
				Utils.CPU_CORES * 2, new DefaultThreadFactory("business-" + nodeId));

		public WorkerSocketChannelHandler(SslContext sslCtx) {
			this.sslCtx = sslCtx;
			this.globalSocketChannelStatisticsHandler = new GlobalSocketChannelStatisticsHandler(metricsRegistry);
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
			pl.addLast("idleStateHandler", new ServerIdleStateHandler(nodeId, nettyConfig.get().getReaderIdleTime()));
			pl.addLast("globalSocketChannelStatisticsHandler", globalSocketChannelStatisticsHandler);
			pl.addLast("protobufFrameDecoder", new ProtobufVarint32FrameDecoder());
			pl.addLast("protobufProtocolDecoder", SharableChannelHandler.PROTOBUF_PROTOCOL_DECODER_REQUEST);
			pl.addLast("authHandler", authHandler);
			pl.addLast(businessExecutor, "serviceHandler", serviceHandler);
		}
	}
}
