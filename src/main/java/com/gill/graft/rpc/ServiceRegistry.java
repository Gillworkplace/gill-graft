package com.gill.graft.rpc;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * ServerRegistry
 *
 * @author gill
 * @version 2023/10/16
 **/
public class ServiceRegistry {

	private final Map<Long, Function<byte[], byte[]>> serviceMap = new ConcurrentHashMap<>();

	/**
	 * 注册服务
	 * 
	 * @param serviceId
	 *            serviceId
	 * @param serviceHandler
	 *            serviceHandler
	 */
	public void register(long serviceId, Function<byte[], byte[]> serviceHandler) {
		serviceMap.put(serviceId, serviceHandler);
	}

	/**
	 * 根据serviceId转换参数
	 * 
	 * @param serviceId
	 *            serviceId
	 */
	public Function<byte[], byte[]> get(long serviceId) {
		return serviceMap.get(serviceId);
	}
}
