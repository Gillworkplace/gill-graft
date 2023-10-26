package com.gill.graft.rpc.handler;

import java.util.concurrent.TimeUnit;

import com.gill.graft.config.RaftConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.AttributeKey;

/**
 * ServerIdleStateHandler
 *
 * @author gill
 * @version 2023/10/22
 **/
public class ServerIdleStateHandler extends IdleStateHandler {

	private static final Logger log = LoggerFactory.getLogger(ServerIdleStateHandler.class);

	private final int nodeId;

	public ServerIdleStateHandler(int nodeId, long readerIdleTime) {
		super(readerIdleTime, 0, 0, TimeUnit.MILLISECONDS);
		this.nodeId = nodeId;
	}

	@Override
	protected void channelIdle(ChannelHandlerContext ctx, IdleStateEvent evt) throws Exception {
		if (evt == IdleStateEvent.FIRST_READER_IDLE_STATE_EVENT) {
			Integer remoteId = ctx.channel().attr(AttributeKey.<Integer>valueOf("nodeId")).get();
			log.info("Idle check happen, {} => {}'s connection is closed", remoteId, nodeId);
			ctx.close();
			return;
		}
		super.channelIdle(ctx, evt);
	}
}
