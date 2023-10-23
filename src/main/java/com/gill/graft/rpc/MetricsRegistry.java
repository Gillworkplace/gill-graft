package com.gill.graft.rpc;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.jmx.JmxReporter;

/**
 * MestricHandler
 *
 * @author gill
 * @version 2023/10/19
 **/
public class MetricsRegistry {

	private final MetricRegistry metricRegistry = new MetricRegistry();

	private final int nodeId;

	private final JmxReporter jmxReporter;

	public MetricsRegistry(int nodeId) {
		this.nodeId = nodeId;
		jmxReporter = JmxReporter.forRegistry(metricRegistry).build();
		jmxReporter.start();
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
		metricRegistry.remove(name);
		metricRegistry.register(name + "-" + nodeId, gauge);
	}

	/**
	 * 关闭jmx资源
	 */
	public void remove() {
		jmxReporter.close();
	}
}
