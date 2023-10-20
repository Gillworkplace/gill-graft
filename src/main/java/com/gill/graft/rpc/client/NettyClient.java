package com.gill.graft.rpc.client;

import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import com.gill.graft.rpc.handler.SharableChannelHandler;
import io.netty.channel.ChannelHandler;
import io.netty.util.concurrent.DefaultThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gill.graft.common.Utils;
import com.gill.graft.config.RaftConfig;
import com.gill.graft.rpc.ConnectionDock;
import com.gill.graft.rpc.codec.Request;
import com.gill.graft.rpc.codec.RequestEncoder;
import com.gill.graft.rpc.codec.Response;
import com.gill.graft.rpc.codec.ResponseDecoder;
import com.google.protobuf.Internal;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.protobuf.ProtobufVarint32FrameDecoder;
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

	private volatile boolean hasShutdown = false;

	private final Supplier<RaftConfig> supplyConfig;

	private final ConnectionDock dock = new ConnectionDock();

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
		while (!hasShutdown) {
			Utils.sleepQuietly(50);
		}
	}

	private void doConnectAndPark() {
		Bootstrap b = new Bootstrap();
		NioEventLoopGroup group = new NioEventLoopGroup(0, new DefaultThreadFactory("netty-client"));
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

		private final ChannelHandler requestPreHandler = new RequestEncoder(dock);

		private final ChannelHandler responsePreHandler = new ResponseDecoder(dock);

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
			pl.addLast("responsePreHandler", responsePreHandler);
		}
	}
}
