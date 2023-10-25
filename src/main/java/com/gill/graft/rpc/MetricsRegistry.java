package com.gill.graft.rpc;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.jmx.JmxReporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * MestricHandler
 *
 * @author gill
 * @version 2023/10/19
 **/
public class MetricsRegistry {

	private final static Logger log = LoggerFactory.getLogger(MetricsRegistry.class);

	private final MetricRegistry metricRegistry = new MetricRegistry();

	private final int nodeId;

	private final JmxReporter jmxReporter;

	public MetricsRegistry(int nodeId) {
		this.nodeId = nodeId;
		jmxReporter = JmxReporter.forRegistry(metricRegistry).build();
	}

	/**
	 * 启动jmxReporter
	 */
	public void start() {
		log.info("{} jmx reporter start.", nodeId);
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
		log.info("{} jmx reporter register gauge: {}.", nodeId, name);
		metricRegistry.remove(name + "-" + nodeId);
		metricRegistry.register(name + "-" + nodeId, gauge);
	}

	/**
	 * 关闭jmx资源
	 */
	public void remove() {
		log.info("{} jmx reporter close.", nodeId);
		jmxReporter.close();
	}
}
