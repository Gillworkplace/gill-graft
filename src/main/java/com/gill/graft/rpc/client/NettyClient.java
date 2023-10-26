package com.gill.graft.rpc.client;

import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
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
import com.gill.graft.rpc.handler.AuthResponseHandler;
import com.gill.graft.rpc.handler.SharableChannelHandler;
import com.google.protobuf.ByteString;
import com.google.protobuf.Internal;

import cn.hutool.core.util.RandomUtil;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.protobuf.ProtobufVarint32FrameDecoder;
import io.netty.util.concurrent.DefaultThreadFactory;

/**
 * NettyClient
 *
 * @author gill
 * @version 2023/10/11
 **/
public class NettyClient {

	private static final Logger log = LoggerFactory.getLogger(NettyClient.class);

	private final int nodeId;

	private volatile int remoteId = -1;

	private final String host;

	private final int port;

	private final Supplier<RaftConfig.NettyConfig> nettyConfig;

	private final Supplier<RaftConfig.AuthConfig> authConfig;

	private final ConnectionDock dock = new ConnectionDock();

	private final ThreadPoolExecutor executor;

	private volatile Channel sc;

	/**
	 * 表示连接是否就绪，包括授权
	 */
	private final AtomicBoolean ready = new AtomicBoolean(false);

	/**
	 * 连接loop是否跳出循环
	 */
	private volatile boolean shutdown = false;

	/**
	 * 用于同步等待连接线程是否运行完成
	 */
	private volatile boolean hasShutdown = false;

	private final AtomicInteger connectionCnt = new AtomicInteger(0);

	public NettyClient(int nodeId, String host, int port, Supplier<RaftConfig.NettyConfig> nettyConfig,
			Supplier<RaftConfig.AuthConfig> authConfig) {
		this.nodeId = nodeId;
		this.host = host;
		this.port = port;
		this.nettyConfig = nettyConfig;
		this.authConfig = authConfig;
		this.executor = new ThreadPoolExecutor(1, 1, 0, TimeUnit.MILLISECONDS,
				new LinkedBlockingQueue<>(Collections.emptyList()), r -> new Thread(r, "netty-connector-" + nodeId),
				new ThreadPoolExecutor.DiscardPolicy());
	}

	public void ready() {
		ready.set(true);
	}

	private void notReady() {
		ready.set(false);
	}

	public boolean isReady() {
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
		if (checkConnection()) {
			Request request = new Request(serviceId, requestB);
			try {
				sc.writeAndFlush(request);
				return Optional.ofNullable(request.getResponse(nettyConfig)).map(Response::getData)
						.orElse(Internal.EMPTY_BYTE_ARRAY);
			} catch (Exception e) {
				log.error("request to {} {}:{} serviceId: {} failed, e: {}", getRemoteId(), host, port, serviceId,
						e.getMessage());
			}
		} else {
			log.error("connection to {}:{} is closed", host, port);
		}
		return Internal.EMPTY_BYTE_ARRAY;
	}

	private boolean checkConnection() {
		if (sc == null || !sc.isOpen()) {
			connect();
			Utils.sleepQuietly(50);
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

	/**
	 * shutdown
	 */
	public void shutdownSync() {
		log.info("{} => {} connection is stopping", nodeId, remoteId);
		shutdown = true;
		dock.release();
		executor.shutdownNow();
		while (!hasShutdown) {
			Utils.sleepQuietly(50);
		}
	}

	private void doConnectAndPark() {
		Bootstrap bs = new Bootstrap();
		NioEventLoopGroup group = new NioEventLoopGroup(0, new DefaultThreadFactory("netty-client-" + nodeId));
		Channel c = null;
		try {
			bs.group(group);
			setOptions(bs);
			setChannel(bs);

			long authId = RandomUtil.randomLong();
			RequestEncoder requestPreHandler = new RequestEncoder(dock);
			AuthResponseHandler authResponseHandler = new AuthResponseHandler(this, authId);
			ResponseDecoder responsePreHandler = new ResponseDecoder(dock);
			bs.handler(new ChannelInitializer<SocketChannel>() {

				@Override
				protected void initChannel(SocketChannel sc) {
					ChannelPipeline pl = sc.pipeline();

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
			});
			ChannelFuture cf = bs.connect(host, port);
			boolean ret = cf.awaitUninterruptibly(nettyConfig.get().getConnectTimeout(), TimeUnit.MILLISECONDS);
			if (ret && cf.isSuccess()) {
				connectionCnt.incrementAndGet();
				log.info("connection to {}:{} is successful", host, port);
				c = cf.channel();

				// 进行连接授权与认证
				auth(c, authId);
				sc = c;
				c.closeFuture().sync();
				log.warn("client to {} {}:{} is closed", getRemoteId(), host, port);
			} else {
				Throwable cause = cf.cause();
				log.error("connection to {}:{} is failed, cause: {}", host, port, cause.getMessage());
			}
		} catch (InterruptedException e) {
			log.warn("client to {} {}:{} interrupted, e: {}", getRemoteId(), host, port, e.getMessage());
		} finally {
			notReady();
			sc = null;
			group.shutdownGracefully().syncUninterruptibly();
			if (c != null && c.isOpen()) {
				try {
					c.close();
				} catch (Exception ignored) {
				}
			}
		}
	}

	private void setChannel(Bootstrap bs) {
		if (Utils.isLinux()) {
			bs.channel(EpollSocketChannel.class);
		} else {
			bs.channel(NioSocketChannel.class);
		}
	}

	private void setOptions(Bootstrap bs) {
		bs.option(ChannelOption.TCP_NODELAY, true).option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);
	}

	private void auth(Channel c, long authId) {
		RaftConfig.AuthConfig config = authConfig.get();
		Raft.Auth auth = Raft.Auth.newBuilder().setNodeId(nodeId).setAuthKey(config.getAuthKey())
				.setAuthValue(ByteString.copyFrom(config.getAuthValue())).build();
		Raft.Request request = Raft.Request.newBuilder().setRequestId(authId).setServiceId(0)
				.setData(auth.toByteString()).build();
		c.writeAndFlush(request);
	}

	/**
	 * 获取连接次数
	 *
	 * @return connectionCnt
	 */
	public int getConnectionCnt() {
		return connectionCnt.get();
	}

	public int getRemoteId() {
		return remoteId;
	}

	public void setRemoteId(int remoteId) {
		this.remoteId = remoteId;
	}

	public int getNodeId() {
		return nodeId;
	}

	public String getHost() {
		return host;
	}

	public int getPort() {
		return port;
	}

	public ConnectionDock getDock() {
		return dock;
	}
}
