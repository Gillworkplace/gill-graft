package com.gill.graft.rpc.server;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gill.graft.Node;
import com.gill.graft.common.Utils;
import com.gill.graft.proto.Raft;
import com.gill.graft.rpc.ServiceRegistry;
import com.google.protobuf.ByteString;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

/**
 * RaftServiceHandler
 *
 * @author gill
 * @version 2023/10/16
 **/
public class RaftServiceHandler extends SimpleChannelInboundHandler<Raft.Request> {

	private static final Logger log = LoggerFactory.getLogger(RaftServiceHandler.class);

	private final Node node;

	private final ExecutorService handlers;

	public RaftServiceHandler(Node node) {
		this.node = node;
		this.handlers = new ThreadPoolExecutor(Utils.CPU_CORES * 2 + 1, Utils.CPU_CORES * 4 + 2, 600, TimeUnit.SECONDS,
				new LinkedBlockingQueue<>(20), r -> new Thread(r, "netty-handler-" + node.getId()));
	}

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, Raft.Request request) {
		ServiceRegistry serviceRegistry = node.getServiceRegistry();
		long requestId = request.getRequestId();
		int serviceId = request.getServiceId();
		log.trace("receive request, id: {}, service: {}", requestId, serviceId);
		ByteString data = request.getData();
		Function<byte[], byte[]> service = serviceRegistry.get(serviceId);
		handlers.execute(() -> {
			byte[] responseData = service.apply(data.toByteArray());
			Raft.Response response = Raft.Response.newBuilder().setRequestId(requestId)
					.setData(ByteString.copyFrom(responseData)).build();
			ctx.writeAndFlush(response);
		});
	}
}
