package com.gill.graft.rpc.handler;

import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;

/**
 * ServerIdleStateHandler
 *
 * @author gill
 * @version 2023/10/22
 **/
public class ServerIdleStateHandler extends IdleStateHandler {

	private static final Logger log = LoggerFactory.getLogger(ServerIdleStateHandler.class);

	private final int nodeId;

	public ServerIdleStateHandler(int nodeId) {
		super(10, 0, 0, TimeUnit.SECONDS);
		this.nodeId = nodeId;
	}

	@Override
	protected void channelIdle(ChannelHandlerContext ctx, IdleStateEvent evt) throws Exception {
		if (evt == IdleStateEvent.FIRST_READER_IDLE_STATE_EVENT) {
			log.info("Idle check happen, nodeId: {}'s connection is closed", nodeId);
			ctx.close();
			return;
		}
		super.channelIdle(ctx, evt);
	}
}
