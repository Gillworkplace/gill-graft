package com.gill.graft.rpc.client;

import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gill.graft.common.Utils;
import com.gill.graft.config.RaftConfig;
import com.gill.graft.proto.Raft;
import com.gill.graft.rpc.ConnectionDock;
import com.gill.graft.rpc.codec.Request;
import com.gill.graft.rpc.codec.RequestEncoder;
import com.gill.graft.rpc.codec.Response;
import com.gill.graft.rpc.codec.ResponseDecoder;
import com.gill.graft.rpc.handler.SharableChannelHandler;
import com.google.protobuf.ByteString;
import com.google.protobuf.Internal;
import com.google.protobuf.InvalidProtocolBufferException;

import cn.hutool.core.util.RandomUtil;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.protobuf.ProtobufVarint32FrameDecoder;
import io.netty.handler.ssl.SslContext;
import io.netty.util.concurrent.DefaultThreadFactory;

/**
 * NettyClient
 *
 * @author gill
 * @version 2023/10/11
 **/
public class NettyClient {

	private static final Logger log = LoggerFactory.getLogger(NettyClient.class);

	private final String host;

	private final int port;

	private final Supplier<RaftConfig> supplyConfig;

	private final ConnectionDock dock = new ConnectionDock();

	private final ThreadPoolExecutor executor;

	private volatile Channel sc;

	/**
	 * 表示连接是否就绪，包括授权
	 */
	private final AtomicBoolean ready = new AtomicBoolean(false);

	private final long authId = RandomUtil.randomLong();

	/**
	 * 连接loop是否跳出循环
	 */
	private volatile boolean shutdown = false;

	/**
	 * 用于同步等待连接线程是否运行完成
	 */
	private volatile boolean hasShutdown = false;

	private final Semaphore continued = new Semaphore(0);

	private final Lock channelLock = new ReentrantLock();

	public NettyClient(String host, int port, Supplier<RaftConfig> supplyConfig) {
		this.host = host;
		this.port = port;
		this.supplyConfig = supplyConfig;
		this.executor = new ThreadPoolExecutor(1, 1, 0, TimeUnit.MILLISECONDS,
				new LinkedBlockingQueue<>(Collections.emptyList()), r -> new Thread(r, "netty-connector"),
				new ThreadPoolExecutor.DiscardPolicy());
	}

	private boolean isReady() {
		return ready.get();
	}

	/**
	 * 请求
	 * 
	 * @param serviceId
	 *            serviceId
	 * @param requestB
	 *            request bytes
	 * @return response bytes
	 */
	public byte[] request(int serviceId, byte[] requestB) {
		if (!checkConnection()) {
			log.error("connection is closed");
			return Internal.EMPTY_BYTE_ARRAY;
		}
		Request request = new Request(serviceId, requestB);
		try {
			sc.writeAndFlush(request);
			return Optional.ofNullable(request.getResponse(supplyConfig)).map(Response::getData)
					.orElse(Internal.EMPTY_BYTE_ARRAY);
		} catch (Exception e) {
			log.error("request serviceId: {} failed, e: {}", serviceId, e.getMessage());
		}
		return Internal.EMPTY_BYTE_ARRAY;
	}

	private boolean checkConnection() {
		if (sc == null || !sc.isOpen()) {
			synchronized (this) {
				long now = System.currentTimeMillis();
				while (!isReady() && System.currentTimeMillis() <= now + supplyConfig.get().getConnectTimeout()) {

					// 线程池的有且只有一个任务，多余的任务会被discard掉，因此不需担心重复执行。
					connect();
					Utils.sleepQuietly(50);
				}
			}
		}
		return isReady() && sc != null && sc.isOpen();
	}

	/**
	 * connect
	 */
	public void connect() {
		executor.execute(() -> {
			hasShutdown = false;
			while (!shutdown) {
				doConnectAndPark();
				Utils.sleepQuietly(1000);
			}
			hasShutdown = true;
		});
	}

	private void awaitConnection(Channel channel) {
		if (channel != sc) {
			return;
		}
		awaitChannel(channel);
	}

	private void awaitChannel(Channel channel) {
		channelLock.lock();
		try {
			if (sc == channel) {
				continued.release();
			}
		} finally {
			channelLock.unlock();
		}
	}

	/**
	 * shutdown
	 */
	public void shutdownSync() {
		shutdown = true;
		dock.release();
		executor.shutdownNow();
		while (!hasShutdown) {
			Utils.sleepQuietly(50);
		}
	}

	private void doConnectAndPark() {
		Bootstrap bs = new Bootstrap();
		NioEventLoopGroup group = new NioEventLoopGroup(0, new DefaultThreadFactory("netty-client"));
		Channel c = null;
		try {
			bs.group(group).handler(new ClientInitializer(null));
			if (Utils.isLinux()) {
				bs.channel(EpollSocketChannel.class);
			} else {
				bs.channel(NioSocketChannel.class);
			}
			c = bs.connect(host, port).sync().channel();

			// 进行连接授权与认证
			auth(c);
			disposeChannel(c);
			continued.acquire();
		} catch (InterruptedException e) {
			log.error("{}:{} client interrupted, e: {}", host, port, e.getMessage());
		} finally {
			ready.set(false);
			group.shutdownGracefully();
			if (c != null) {
				c.close();
			}
		}
	}

	private void disposeChannel(Channel c) {
		channelLock.lock();
		try {
			sc = c;
			continued.drainPermits();
		} finally {
			channelLock.unlock();
		}
	}

	private void auth(Channel c) {
		RaftConfig.AuthConfig authConfig = supplyConfig.get().getAuthConfig();
		Raft.Auth auth = Raft.Auth.newBuilder().setAuthKey(authConfig.getAuthKey())
				.setAuthValue(ByteString.copyFrom(authConfig.getAuthValue())).build();
		Raft.Request request = Raft.Request.newBuilder().setRequestId(authId).setServiceId(0)
				.setData(auth.toByteString()).build();
		c.writeAndFlush(request);
	}

	class ClientInitializer extends ChannelInitializer<SocketChannel> {

		private final SslContext sslCtx;

		private final RequestEncoder requestPreHandler = new RequestEncoder(dock);

		private final AuthResponseHandler authResponseHandler = new AuthResponseHandler();

		private final ResponseDecoder responsePreHandler = new ResponseDecoder(dock);

		public ClientInitializer(SslContext sslCtx) {
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
			pl.addLast("requestPreHandler", requestPreHandler);

			// in
			pl.addLast("protobufFrameDecoder", new ProtobufVarint32FrameDecoder());
			pl.addLast("protobufProtocolDecoder", SharableChannelHandler.PROTOBUF_PROTOCOL_DECODER_RESPONSE);
			pl.addLast("authResponseHandler", authResponseHandler);
			pl.addLast("responsePreHandler", responsePreHandler);
		}

		@Override
		public void channelInactive(ChannelHandlerContext ctx) throws Exception {
			awaitConnection(ctx.channel());
			super.channelInactive(ctx);
		}
	}

	@ChannelHandler.Sharable
	class AuthResponseHandler extends SimpleChannelInboundHandler<Raft.Response> {

		@Override
		protected void channelRead0(ChannelHandlerContext ctx, Raft.Response msg) {
			long requestId = msg.getRequestId();
			try {
				Raft.AuthResponse authResponse = Raft.AuthResponse.parseFrom(msg.getData());
				if (requestId == authId && authResponse.getSuccess()) {
					ready.compareAndSet(false, true);
					log.info("connection to {}:{} success", host, port);
					return;
				}
				dock.release();
			} catch (InvalidProtocolBufferException ignore) {
			} finally {
				ctx.pipeline().remove(this);
			}
			log.error("connection auth failed by {}:{}", host, port);
			ctx.close();
		}
	}
}
