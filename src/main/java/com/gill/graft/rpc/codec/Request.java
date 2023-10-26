package com.gill.graft.rpc.codec;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import com.gill.graft.config.RaftConfig;

/**
 * Request
 *
 * @author gill
 * @version 2023/10/17
 **/
public class Request {

	private int serviceId;

	private byte[] data;

	private final CompletableFuture<Response> cf;

	public Request(int serviceId, byte[] data) {
		this.serviceId = serviceId;
		this.data = data;
		this.cf = new CompletableFuture<>();
	}

	public int getServiceId() {
		return serviceId;
	}

	public void setServiceId(int serviceId) {
		this.serviceId = serviceId;
	}

	public byte[] getData() {
		return data;
	}

	public void setData(byte[] data) {
		this.data = data;
	}

	public CompletableFuture<Response> getCf() {
		return cf;
	}

	/**
	 * 获取响应结果
	 * 
	 * @param supplyConfig
	 *            配置
	 * @return 结果
	 */
	public Response getResponse(Supplier<RaftConfig.NettyConfig> supplyConfig) {
		try {
			return cf.get(supplyConfig.get().getRequestTimeout(), TimeUnit.MILLISECONDS);
		} catch (Exception ignored) {
		}
		return null;
	}
}
