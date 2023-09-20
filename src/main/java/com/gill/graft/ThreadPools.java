package com.gill.graft;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.gill.graft.common.Utils;

/**
 * NodePool
 *
 * @author gill
 * @version 2023/09/06
 **/
public class ThreadPools {

	private final Lock clusterPoolLock = new ReentrantLock();

	private ExecutorService clusterPool;

	private final Lock apiPoolLock = new ReentrantLock();

	private ExecutorService apiPool;

	public ExecutorService getClusterPool() {
		return clusterPool;
	}

	public ExecutorService getApiPool() {
		return apiPool;
	}

	/**
	 * set
	 * 
	 * @param clusterPool
	 *            线程池
	 */
	public void setClusterPool(ExecutorService clusterPool) {
		ExecutorService tmp = this.clusterPool;
		clusterPoolLock.lock();
		try {
			this.clusterPool = clusterPool;
		} finally {
			clusterPoolLock.unlock();
		}
		if (tmp != null) {
			tmp.shutdownNow();
			Utils.awaitTermination(tmp, "clusterPool");
		}
	}

	/**
	 * set
	 *
	 * @param apiPool
	 *            线程池
	 */
	public void setApiPool(ExecutorService apiPool) {
		ExecutorService tmp = this.apiPool;
		apiPoolLock.lock();
		try {
			this.apiPool = apiPool;
		} finally {
			apiPoolLock.unlock();
		}
		if (tmp != null) {
			tmp.shutdownNow();
			Utils.awaitTermination(tmp, "apiPool");
		}
	}
}
