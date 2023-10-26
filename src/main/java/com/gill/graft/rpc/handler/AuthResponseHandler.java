package com.gill.graft.rpc.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gill.graft.proto.Raft;
import com.gill.graft.rpc.client.NettyClient;
import com.google.protobuf.InvalidProtocolBufferException;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

/**
 * AuthResponseHandler
 *
 * @author gill
 * @version 2023/10/25
 **/
@ChannelHandler.Sharable
public class AuthResponseHandler extends SimpleChannelInboundHandler<Raft.Response> {

	private static final Logger log = LoggerFactory.getLogger(AuthResponseHandler.class);

	private final NettyClient nettyClient;

	private final long authId;

	public AuthResponseHandler(NettyClient nettyClient, long authId) {
		this.nettyClient = nettyClient;
		this.authId = authId;
	}

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, Raft.Response msg) {
		long requestId = msg.getRequestId();
		try {
			Raft.AuthResponse authResponse = Raft.AuthResponse.parseFrom(msg.getData());
			if (requestId == authId && authResponse.getSuccess()) {
				nettyClient.ready();
				nettyClient.setRemoteId(authResponse.getNodeId());
				log.info("{} auth to {} {}:{} success", nettyClient.getNodeId(), nettyClient.getRemoteId(),
						nettyClient.getHost(), nettyClient.getPort());
				return;
			}
			nettyClient.getDock().release();
		} catch (InvalidProtocolBufferException ignore) {
		} finally {
			ctx.pipeline().remove(this);
		}
		log.error("{} auth failed by {}:{}", nettyClient.getNodeId(), nettyClient.getHost(), nettyClient.getPort());
		ctx.close();
	}
}
