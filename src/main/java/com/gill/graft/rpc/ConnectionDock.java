package com.gill.graft.rpc;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import com.gill.graft.config.RaftConfig;
import com.gill.graft.rpc.codec.Response;

/**
 * ConnectionDock
 *
 * @author gill
 * @version 2023/10/17
 **/
public class ConnectionDock {

	private final Map<Long, CompletableFuture<Response>> resps = new ConcurrentHashMap<>(1024);


	/**
	 * 添加等待的请求
	 * 
	 * @param requestId
	 *            请求id
	 * @param cf
	 *            future
	 */
	public void add(long requestId, CompletableFuture<Response> cf) {
		resps.put(requestId, cf);
	}

	/**
	 * 请求响应
	 * 
	 * @param requestId
	 *            请求id
	 * @param resp
	 *            响应结果
	 */
	public void complete(long requestId, Response resp) {
		CompletableFuture<Response> cf = resps.get(requestId);
		if (cf != null) {
			cf.complete(resp);
			resps.remove(requestId);
		}
	}
}
