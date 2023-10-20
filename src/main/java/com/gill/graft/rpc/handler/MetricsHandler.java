package com.gill.graft.rpc.handler;

import java.util.concurrent.atomic.AtomicInteger;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.jmx.JmxReporter;

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
public class MetricsHandler extends ChannelDuplexHandler {

	private final AtomicInteger connCnt = new AtomicInteger(0);

	private final MetricRegistry metricRegistry = new MetricRegistry();

	public MetricsHandler(int nodeId) {
		MetricRegistry metricRegistry = new MetricRegistry();
		JmxReporter jmxReporter = JmxReporter.forRegistry(metricRegistry).build();
		jmxReporter.start();
		register("connectionCount-" + nodeId, connCnt::get);
	}

	/**
	 * 注册netty指标数据
	 * 
	 * @param name
	 *            name
	 * @param gauge
	 *            指标
	 * @param <T>
	 *            类型
	 */
	public <T> void register(String name, Gauge<T> gauge) {
		metricRegistry.register(name, gauge);
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
