package com.gill.graft.rpc.handler;

import java.util.concurrent.atomic.AtomicInteger;

import com.gill.graft.rpc.MetricsRegistry;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;

/**
 * MestricHandler
 *
 * @author gill
 * @version 2023/10/19
 **/
@ChannelHandler.Sharable
public class GlobalSocketChannelStatisticsHandler extends ChannelDuplexHandler {

	private final AtomicInteger connCnt = new AtomicInteger(0);

	public GlobalSocketChannelStatisticsHandler(MetricsRegistry registry) {
		registry.register("connectionCount", connCnt::get);
	}

	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		connCnt.incrementAndGet();
		super.channelActive(ctx);
	}

	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		connCnt.decrementAndGet();
		super.channelInactive(ctx);
	}
}
