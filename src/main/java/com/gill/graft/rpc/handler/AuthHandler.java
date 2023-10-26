package com.gill.graft.rpc.handler;

import java.util.Arrays;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gill.graft.config.RaftConfig;
import com.gill.graft.proto.Raft;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.AttributeKey;

/**
 * AuthHandler
 *
 * @author gill
 * @version 2023/10/23
 **/
@ChannelHandler.Sharable
public class AuthHandler extends SimpleChannelInboundHandler<Raft.Request> {

	private static final Logger log = LoggerFactory.getLogger(AuthHandler.class);

	private final int nodeId;

	private final Supplier<RaftConfig.AuthConfig> authConfig;

	public AuthHandler(int nodeId, Supplier<RaftConfig.AuthConfig> authConfig) {
		this.nodeId = nodeId;
		this.authConfig = authConfig;
	}

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, Raft.Request msg) {
		try {
			long requestId = msg.getRequestId();
			Raft.Auth auth = Raft.Auth.parseFrom(msg.getData());
			RaftConfig.AuthConfig config = authConfig.get();
			Raft.Response.Builder responseBuilder = Raft.Response.newBuilder().setRequestId(requestId);
			if (ignoreAuth(config) || auth(auth, config)) {
				log.info("{} auth success", ctx.channel().remoteAddress());
				ctx.channel().attr(AttributeKey.<Integer>valueOf("nodeId")).set(auth.getNodeId());
				ctx.writeAndFlush(responseBuilder.setData(authR(true)).build());
				return;
			}
			ctx.writeAndFlush(responseBuilder.setData(authR(false)).build());
			log.error("{} auth failed", ctx.channel().remoteAddress());
		} catch (InvalidProtocolBufferException ignore) {
		} finally {
			ctx.pipeline().remove(this);
		}
		ctx.close();
	}

	private ByteString authR(boolean value) {
		return Raft.AuthResponse.newBuilder().setNodeId(nodeId).setSuccess(value).build().toByteString();
	}

	private static boolean auth(Raft.Auth auth, RaftConfig.AuthConfig authConfig) {
		return authConfig.getAuthKey() == auth.getAuthKey()
				&& Arrays.equals(authConfig.getAuthValue(), auth.getAuthValue().toByteArray());
	}

	private static boolean ignoreAuth(RaftConfig.AuthConfig authConfig) {
		return authConfig.getAuthKey() == 0;
	}
}
