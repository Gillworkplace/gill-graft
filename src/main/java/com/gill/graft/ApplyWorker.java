package com.gill.graft;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gill.graft.apis.DataStorage;
import com.gill.graft.common.Utils;
import com.gill.graft.model.LogEntry;
import com.gill.graft.service.PrintService;

import cn.hutool.core.thread.NamedThreadFactory;

/**
 * ConnectionDock
 *
 * @author gill
 * @version 2023/10/17
 **/
public class ApplyWorker implements PrintService {

	private static final Logger log = LoggerFactory.getLogger(ApplyWorker.class);

	private final DataStorage dataStorage;

	private final LogManager logManager;

	private final Supplier<Integer> applyIdx;

	private final Supplier<Integer> commitIdx;

	private final Map<Integer, ApplyCallback> callbacks = new ConcurrentHashMap<>();

	private volatile boolean running = true;

	private final ExecutorService executor;

	private volatile Thread executorThread;

	public ApplyWorker(int nodeId, DataStorage dataStorage, LogManager logManager, Supplier<Integer> applyIdx,
			Supplier<Integer> commitIdx) {
		this.dataStorage = dataStorage;
		this.logManager = logManager;
		this.applyIdx = applyIdx;
		this.commitIdx = commitIdx;
		this.executor = new ThreadPoolExecutor(1, 1, 0, TimeUnit.SECONDS, new SynchronousQueue<>(),
				new NamedThreadFactory("apply-worker-" + nodeId + "-", false));
	}

	static class ApplyCallback {

		private final int logIdx;

		private final CompletableFuture<Object> cf = new CompletableFuture<>();

		ApplyCallback(int logIdx) {
			this.logIdx = logIdx;
		}

		void complete(Object ret) {
			cf.complete(ret);
		}

		void cancel() {
			cf.cancel(true);
		}

		CompletableFuture<Object> getCf() {
			return cf;
		}

		@Override
		public String toString() {
			int state = 0;
			state |= cf.isDone() ? 1 : 0;
			state |= cf.isCancelled() ? 1 << 1 : 0;
			return "ApplyTask{" + "logIdx=" + logIdx + ',' + "state=" + Integer.toBinaryString(state) + '}';
		}
	}

	/**
	 * 唤醒apply worker
	 */
	public void wakeup() {
		LockSupport.unpark(executorThread);
	}

	public void start() {
		running = true;
		executor.execute(() -> {
			log.info("apply-worker is started");
			executorThread = Thread.currentThread();
			outer : while (running) {
				int count = 0;

				// 正常情况下applyIdx会小于等于commitIdx
				// 当applyIdx等于commitIdx时说明日志已全部同步完成
				while (commitIdx.get() <= applyIdx.get()) {
					if (++count > 16) {
						LockSupport.park();
					}
					Thread.yield();
					if (!running) {
						break outer;
					}
				}
				LogEntry logEntry = logManager.getLog(applyIdx.get() + 1);
				if (logEntry == null) {
					Utils.sleepQuietly(10);
					continue;
				}
				int logIdx = logEntry.getIndex();
				long logTerm = logEntry.getTerm();
				String command = logEntry.getCommand();
				log.debug("data storage apply idx: {}, command: {}", logIdx, command);
				Object ret = dataStorage.apply(logTerm, logIdx, command);
				Optional.ofNullable(callbacks.get(logIdx)).ifPresent(callback -> callback.complete(ret));
				Utils.sleepQuietly(10);
			}
			log.info("apply-worker is stopped");
		});
	}

	public void stop() {
		log.info("apply-worker is stopping");
		running = false;
		LockSupport.unpark(executorThread);
		for (ApplyCallback task : callbacks.values()) {
			task.cancel();
		}
	}

	/**
	 * 异步应用日志
	 *
	 * @param logIdx
	 *            日志条目
	 * @return cf
	 */
	public CompletableFuture<Object> registerApplyCallback(int logIdx) {
		ApplyCallback applyTask = new ApplyCallback(logIdx);
		callbacks.put(logIdx, applyTask);
		return applyTask.getCf();
	}

	/**
	 * 打印状态
	 *
	 * @return 内容
	 */
	@Override
	public String println() {
		return executorThread.getName() + System.lineSeparator() + callbacks;
	}
}
