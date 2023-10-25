package com.gill.graft;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.gill.graft.common.Utils;
import com.gill.graft.config.RaftConfig;
import com.gill.graft.scheduler.SnapshotScheduler;

/**
 * Schedulers
 *
 * @author gill
 * @version 2023/09/05
 **/
public class Schedulers {

	private final Lock timeoutLock = new ReentrantLock();

	private ScheduledExecutorService timeoutScheduler;

	private final Lock heartbeatLock = new ReentrantLock();

	private ScheduledExecutorService heartbeatScheduler;

	private SnapshotScheduler snapshotScheduler = new SnapshotScheduler();

	public ScheduledExecutorService getTimeoutScheduler() {
		return timeoutScheduler;
	}

	public ScheduledExecutorService getHeartbeatScheduler() {
		return heartbeatScheduler;
	}

	public SnapshotScheduler getSnapshotScheduler() {
		return snapshotScheduler;
	}

	/**
	 * set
	 *
	 * @param func
	 *            r
	 * @param interval
	 *            interval
	 * @param nodeId
	 *            节点ID
	 */
	public void setTimeoutScheduler(Runnable func, long interval, int nodeId) {
		timeoutLock.lock();
		try {
			if (this.timeoutScheduler != null) {
				clearTimeoutScheduler();
			}
			this.timeoutScheduler = new ScheduledThreadPoolExecutor(1, r -> new Thread(r, "follower-" + nodeId));
			this.timeoutScheduler.scheduleAtFixedRate(func, interval, interval, TimeUnit.MILLISECONDS);
		} finally {
			timeoutLock.unlock();
		}
	}

	/**
	 * 清除定时任务
	 */
	public void clearTimeoutScheduler() {
		ScheduledExecutorService tmp = this.timeoutScheduler;
		timeoutLock.lock();
		try {
			this.timeoutScheduler = null;
		} finally {
			timeoutLock.unlock();
		}
		if (tmp != null) {
			tmp.shutdownNow();
			Utils.awaitTermination(tmp, "timeoutScheduler");
		}
	}

	/**
	 * set
	 *
	 * @param runnable
	 *            r
	 * @param config
	 *            config
	 * @param nodeId
	 *            节点ID
	 */
	public synchronized void setHeartbeatScheduler(Runnable runnable, RaftConfig config, int nodeId) {
		heartbeatLock.lock();
		try {
			if (this.heartbeatScheduler != null) {
				clearHeartbeatScheduler();
			}
			this.heartbeatScheduler = new ScheduledThreadPoolExecutor(1, r -> new Thread(r, "leader-" + nodeId));
			this.heartbeatScheduler.scheduleAtFixedRate(runnable, 0, config.getHeartbeatInterval(),
					TimeUnit.MILLISECONDS);
		} finally {
			heartbeatLock.unlock();
		}
	}

	/**
	 * 清除定时任务
	 */
	public void clearHeartbeatScheduler() {
		ScheduledExecutorService tmp = this.heartbeatScheduler;
		heartbeatLock.lock();
		try {
			this.heartbeatScheduler = null;
		} finally {
			heartbeatLock.unlock();
		}
		if (tmp != null) {
			tmp.shutdownNow();
			Utils.awaitTermination(tmp, "heartbeatScheduler");
		}
	}
}
