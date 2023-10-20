package com.gill.graft.rpc.codec;

import com.gill.graft.proto.Raft;
import com.gill.graft.rpc.ConnectionDock;
import com.google.protobuf.ByteString;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

/**
 * ResponseEncoder
 *
 * @author gill
 * @version 2023/10/17
 **/
public class ResponseDecoder extends SimpleChannelInboundHandler<Raft.Response> {

	private final ConnectionDock dock;

	public ResponseDecoder(ConnectionDock dock) {
		this.dock = dock;
	}

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, Raft.Response resp) {
		long requestId = resp.getRequestId();
		ByteString data = resp.getData();
		Response response = new Response(data.toByteArray());
		dock.complete(requestId, response);
	}
}
