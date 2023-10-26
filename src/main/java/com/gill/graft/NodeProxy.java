package com.gill.graft;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gill.graft.apis.DataStorage;
import com.gill.graft.apis.RaftRpcService;
import com.gill.graft.common.Utils;
import com.gill.graft.entity.AppendLogEntriesParam;
import com.gill.graft.entity.AppendLogReply;
import com.gill.graft.entity.ReplicateSnapshotParam;
import com.gill.graft.entity.Reply;
import com.gill.graft.exception.SyncSnapshotException;
import com.gill.graft.model.LogEntry;
import com.gill.graft.model.Snapshot;
import com.gill.graft.service.PrintService;

/**
 * NodeProxy
 *
 * @author gill
 * @version 2023/09/11
 **/
public class NodeProxy implements Runnable, PrintService {

	private static final Logger log = LoggerFactory.getLogger(NodeProxy.class);

	private static final long TIMEOUT = 50L;

	private static final int BATCH = 100;

	private final Node self;

	private final RaftRpcService follower;

	private int preLogIdx;

	private final ConcurrentSkipListMap<Integer, LogEntryReply> logs = new ConcurrentSkipListMap<>();

	private final ExecutorService executor;

	private volatile boolean running = true;

	public NodeProxy(Node self, RaftRpcService follower, int lastLogIdx) {
		this.self = self;
		this.follower = follower;
		this.preLogIdx = lastLogIdx;
		this.executor = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(10),
				r -> new Thread(r, "node-proxy-" + self.getId() + "-" + follower.getId()));
	}

	public int getID() {
		return follower.getId();
	}

	@Override
	public String println() {
		StringBuilder sb = new StringBuilder();
		sb.append("NODE-PROXY-").append(follower.getId()).append(System.lineSeparator());
		sb.append("running: ").append(running).append(System.lineSeparator());
		sb.append("preLogIdx: ").append(preLogIdx).append(System.lineSeparator());
		sb.append("waiting append logs: ").append(logs).append(System.lineSeparator());
		return sb.toString();
	}

	/**
	 * 启动
	 */
	public void start() {
		this.running = true;
		this.executor.execute(this);
	}

	/**
	 * 停止
	 */
	public void stop() {
		this.running = false;
		this.executor.shutdownNow();
		Utils.awaitTermination(this.executor, "proxy-" + self.getId() + "-" + follower.getId());
	}

	/**
	 * 单线程运行，无并发问题
	 */
	@Override
	public void run() {
		while (running) {
			if (skip()) {
				continue;
			}
			List<LogEntryReply> entries = pollSuccessiveLogs();
			List<LogEntry> appendLogs = entries.stream().map(LogEntryReply::getLogEntry).collect(Collectors.toList());
			try {
				AppendLogReply reply = doAppendLogs(appendLogs);
				if (handleReply(entries, reply)) {
					continue;
				}
				putbackLogs(entries);
			} catch (Exception e) {
				log.error("node: {} appends logs to {} failed, e: {}", self.getId(), follower.getId(), e.getMessage());
				putbackLogs(entries);
			}
		}
	}

	private boolean skip() {
		Utils.sleepQuietly(TIMEOUT);
		return logs.isEmpty() || logs.firstKey() != preLogIdx + 1;
	}

	private List<LogEntryReply> pollSuccessiveLogs() {
		List<LogEntryReply> entries = new ArrayList<>();
		for (int i = 0, preIdx = preLogIdx; i < BATCH && !logs.isEmpty()
				&& logs.firstKey() == preIdx + 1; i++, preIdx++) {
			entries.add(logs.pollFirstEntry().getValue());
		}
		return entries;
	}

	private AppendLogReply doAppendLogs(List<LogEntry> appendLogs) {
		int nodeId = self.getId();
		long term = self.getTerm();
		long preLogTerm = self.getLogManager().getLog(preLogIdx).getTerm();
		int committedIdx = self.getCommittedIdx();
		AppendLogEntriesParam param = new AppendLogEntriesParam(nodeId, term, preLogTerm, preLogIdx, committedIdx,
				appendLogs);
		log.debug("node: {} proposes to {}, logs: {}, committedIdx: {}", nodeId, follower.getId(), appendLogs,
				committedIdx);
		return follower.appendLogEntries(param);
	}

	private boolean handleReply(List<LogEntryReply> entries, AppendLogReply reply) throws SyncSnapshotException {
		if (reply.isSuccess()) {
			handleSuccess(entries, reply);
			return true;
		} else if (reply.getTerm() > self.getTerm()) {

			// 服务端任期大于本机，则更新任期并降级为follower
			self.stepDown(reply.getTerm());
		} else if (reply.isSyncSnapshot()) {

			// 重新同步快照信息及日志
			syncSnapshot(entries);
		} else {

			// 修复follower旧日志
			repairOldLogs(entries, reply.getCompareIdx());
		}
		return false;
	}

	private void handleSuccess(List<LogEntryReply> entries, AppendLogReply reply) {
		preLogIdx = lastLogIdx(entries);
		for (LogEntryReply entry : entries) {
			entry.setReply(reply);
			Optional.ofNullable(entry.getLatch()).ifPresent(CountDownLatch::countDown);
		}
	}

	private void syncSnapshot(List<LogEntryReply> entries) throws SyncSnapshotException {
		int nodeId = self.getId();
		log.debug("node: {} sync snapshot to {}", nodeId, follower.getId());
		DataStorage dataStorage = self.getDataStorage();
		Snapshot snapshot = dataStorage.getSnapshot();
		long term = self.getTerm();
		long applyTerm = snapshot.getApplyTerm();
		int applyIdx = snapshot.getApplyIdx();
		byte[] data = snapshot.getData();
		ReplicateSnapshotParam param = new ReplicateSnapshotParam(nodeId, term, applyIdx, applyTerm, data);
		Reply reply = follower.replicateSnapshot(param);
		if (!reply.isSuccess()) {
			throw new SyncSnapshotException(
					String.format("node: %s sync snapshot to %s failed, term: %s", nodeId, follower.getId(), term));
		}
		log.info("node: {} finish to sync snapshot to {}, applyIdx: {}, applyTerm: {}", nodeId, follower.getId(),
				applyIdx, applyTerm);
		clearLogsBeforeApplyIdx(applyIdx, entries);
	}

	private void clearLogsBeforeApplyIdx(int applyIdx, List<LogEntryReply> entries) {
		for (Integer idx : logs.headMap(applyIdx, true).keySet()) {
			LogEntryReply entry = logs.remove(idx);
			entry.setReply(new AppendLogReply(true, -1));
			Optional.ofNullable(entry.getLatch()).ifPresent(CountDownLatch::countDown);
		}
		entries.removeIf(entry -> entry.getLogEntry().getIndex() <= applyIdx);
	}

	private void repairOldLogs(List<LogEntryReply> entries, int compareIdx) throws SyncSnapshotException {
		if (compareIdx < 0) {
			return;
		}
		DataStorage dataStorage = self.getDataStorage();

		// 如果compareIdx小于 snapshot 的 applyIdx说明同步的日志可能已被删除，直接同步快照
		if (compareIdx < dataStorage.getApplyIdx()) {
			log.debug("node: {} is repairing logs, but needs to sync snapshots", self.getId());
			syncSnapshot(entries);
			return;
		}
		log.debug("node: {} repair logs to {}, compare idx: {}", self.getId(), follower.getId(), compareIdx);

		// 从日志中获取compareIdx到队列第一个元素的所有日志
		LogManager logManager = self.getLogManager();
		int endIdx = Optional.ofNullable(logs.firstEntry()).map(Map.Entry::getKey).orElse(Integer.MAX_VALUE);
		List<LogEntry> logEntries = logManager.getLogs(compareIdx + 1, endIdx);
		for (LogEntry logEntry : logEntries) {
			int logIdx = logEntry.getIndex();
			LogEntryReply entry = new LogEntryReply(null, logEntry);
			this.logs.put(logIdx, entry);
		}
		preLogIdx = compareIdx;
	}

	private void putbackLogs(List<LogEntryReply> entries) {
		for (LogEntryReply entry : entries) {
			logs.put(entry.getLogEntry().getIndex(), entry);
		}
	}

	private static int lastLogIdx(List<LogEntryReply> appendLogs) {
		return appendLogs.get(appendLogs.size() - 1).getLogEntry().getIndex();
	}

	private static class LogEntryReply {

		final CountDownLatch latch;

		final LogEntry logEntry;

		AppendLogReply reply;

		public LogEntryReply(CountDownLatch latch, LogEntry logEntry) {
			this.latch = latch;
			this.logEntry = logEntry;
		}

		public CountDownLatch getLatch() {
			return latch;
		}

		public LogEntry getLogEntry() {
			return logEntry;
		}

		public AppendLogReply getReply() {
			return reply;
		}

		public void setReply(AppendLogReply reply) {
			this.reply = reply;
		}

		@Override
		public String toString() {
			return "LogEntryReply{" + "logEntry=" + logEntry + ", reply=" + reply + '}';
		}
	}

	/**
	 * 追加日志
	 * 
	 * @param logEntry
	 *            日志
	 * @return 是否成功
	 */
	public AppendLogReply appendLog(LogEntry logEntry) {
		CountDownLatch latch = new CountDownLatch(1);
		LogEntryReply entry = new LogEntryReply(latch, logEntry);
		try {
			logs.put(logEntry.getIndex(), entry);
			latch.await();
		} catch (InterruptedException e) {
			log.warn("appendLog interrupted, e: {}", e.toString());
		}
		return entry.getReply();
	}
}
