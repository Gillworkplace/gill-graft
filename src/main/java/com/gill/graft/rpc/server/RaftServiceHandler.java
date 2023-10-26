package com.gill.graft.rpc.server;

import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gill.graft.proto.Raft;
import com.gill.graft.rpc.ServiceRegistry;
import com.google.protobuf.ByteString;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

/**
 * RaftServiceHandler
 *
 * @author gill
 * @version 2023/10/16
 **/
@ChannelHandler.Sharable
public class RaftServiceHandler extends SimpleChannelInboundHandler<Raft.Request> {

	private static final Logger log = LoggerFactory.getLogger(RaftServiceHandler.class);

	private final ServiceRegistry serviceRegistry;

	public RaftServiceHandler(ServiceRegistry serviceRegistry) {
		this.serviceRegistry = serviceRegistry;
	}

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, Raft.Request request) {

		// 内存泄露模拟
		// ctx.alloc().buffer();
		// System.gc();
		long requestId = request.getRequestId();
		int serviceId = request.getServiceId();
		log.trace("receive request, id: {}, service: {}", requestId, serviceId);
		ByteString data = request.getData();
		Function<byte[], byte[]> service = serviceRegistry.get(serviceId);
		byte[] responseData = service.apply(data.toByteArray());
		Raft.Response response = Raft.Response.newBuilder().setRequestId(requestId)
				.setData(ByteString.copyFrom(responseData)).build();
		ctx.writeAndFlush(response);
	}
}
