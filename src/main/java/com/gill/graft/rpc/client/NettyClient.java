package com.gill.graft.rpc.client;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gill.graft.common.Utils;
import com.gill.graft.config.RaftConfig;
import com.gill.graft.proto.Raft;
import com.google.protobuf.ByteString;
import com.google.protobuf.Internal;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.protobuf.ProtobufDecoder;
import io.netty.handler.codec.protobuf.ProtobufEncoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32FrameDecoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32LengthFieldPrepender;
import io.netty.handler.ssl.SslContext;

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

	private volatile Channel sc;

	private volatile boolean connected = false;

	private volatile boolean shutdown = false;

	private volatile boolean shutdowned = false;

	private final AtomicLong reqGen = new AtomicLong(0);

	private final Map<Long, ResponsePacket> packets = new ConcurrentHashMap<>(1024);

	private final Supplier<RaftConfig> supplyConfig;

	private final ExecutorService executor;

	public NettyClient(String host, int port, Supplier<RaftConfig> supplyConfig) {
		this.host = host;
		this.port = port;
		this.supplyConfig = supplyConfig;
		this.executor = new ThreadPoolExecutor(1, 1, 0, TimeUnit.MILLISECONDS,
				new LinkedBlockingQueue<>(Collections.emptyList()), r -> new Thread(r, "netty-connector"),
				new ThreadPoolExecutor.DiscardPolicy());
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
			return Internal.EMPTY_BYTE_ARRAY;
		}
		long requestId = reqGen.incrementAndGet();
		Raft.Request request = Raft.Request.newBuilder().setRequestId(requestId).setServiceId(serviceId)
				.setData(ByteString.copyFrom(requestB)).build();
		ResponsePacket pa = new ResponsePacket();
		packets.put(requestId, pa);
		try {
			sc.writeAndFlush(request);

			// 最多等待30s
			if (pa.getLatch().await(30, TimeUnit.SECONDS)) {
				return pa.getResponse().getData().toByteArray();
			}
		} catch (InterruptedException ignored) {
		} catch (Exception e) {
			log.error("request serviceId: {} failed, e: {}", serviceId, e.getMessage());
		} finally {
			packets.remove(requestId);
		}
		return Internal.EMPTY_BYTE_ARRAY;
	}

	private boolean checkConnection() {
		if (sc == null || !sc.isOpen()) {
			synchronized (this) {
				long now = System.currentTimeMillis();
				while (!connected && System.currentTimeMillis() <= now + supplyConfig.get().getConnectTimeout()) {
					connect();
					Utils.sleepQuietly(50);
				}
			}
		}
		return sc != null && sc.isOpen();
	}

	/**
	 * connect
	 * 
	 * @return this
	 */
	public NettyClient connect() {
		executor.execute(() -> {
			shutdowned = false;
			while (!shutdown) {
				doConnectAndPark();
				Utils.sleepQuietly(1000);
			}
			shutdowned = true;
		});
		return this;
	}

	/**
	 * disconnect
	 * 
	 * @return this
	 */
	public NettyClient disconnect() {
		connected = false;
		return this;
	}

	/**
	 * shutdown
	 */
	public void shutdownSync() {
		connected = false;
		shutdown = true;
		executor.shutdownNow();
		while (!shutdowned) {
			Utils.sleepQuietly(50);
		}
	}

	private void doConnectAndPark() {
		Bootstrap b = new Bootstrap();
		NioEventLoopGroup group = new NioEventLoopGroup();
		Channel c = null;
		try {
			b.group(group).channel(NioSocketChannel.class).handler(new ClientInitializer(null));
			c = b.connect(host, port).sync().channel();
			sc = c;
			connected = true;
			while (connected) {
				Utils.sleepQuietly(10L * 60 * 1000);
			}
		} catch (InterruptedException e) {
			log.error("{}:{} client interrupted, e: {}", host, port, e.getMessage());
		} finally {
			group.shutdownGracefully();
			if (c != null) {
				c.close();
			}
		}
	}

	class ClientInitializer extends ChannelInitializer<SocketChannel> {

		private final SslContext sslCtx;

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
			pl.addLast(new ProtobufVarint32LengthFieldPrepender());
			pl.addLast(new ProtobufEncoder());

			// in
			pl.addLast(new ProtobufVarint32FrameDecoder());
			pl.addLast(new ProtobufDecoder(Raft.Response.getDefaultInstance()));
			pl.addLast(new ResponseHandler());
		}
	}

	class ResponseHandler extends SimpleChannelInboundHandler<Raft.Response> {

		@Override
		protected void channelRead0(ChannelHandlerContext ctx, Raft.Response resp) {
			long requestId = resp.getRequestId();
			ResponsePacket pa = packets.get(requestId);
			if (pa != null) {
				pa.setResponse(resp);
				pa.getLatch().countDown();
			}
		}
	}

	static class ResponsePacket {

		private final CountDownLatch latch = new CountDownLatch(1);

		private Raft.Response response;

		public void setResponse(Raft.Response response) {
			this.response = response;
		}

		public CountDownLatch getLatch() {
			return latch;
		}

		public Raft.Response getResponse() {
			return response;
		}
	}
}
