package com.gill.graft.apis;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gill.graft.common.Utils;
import com.gill.graft.model.Snapshot;

import javafx.util.Pair;

/**
 * VersionDataStorage
 *
 * @author gill
 * @version 2023/09/18
 **/
public abstract class VersionDataStorage<T> implements DataStorage {

	private static final Logger log = LoggerFactory.getLogger(VersionDataStorage.class);

	private volatile int lastSnapshotApplyIdx = 0;

	private volatile long applyTerm = 0;

	private volatile int applyIdx = 0;

	private final Lock lock = new ReentrantLock();

	@Override
	public final int loadSnapshot() {
		Pair<Long, Integer> pair = loadApplyTermAndIdx();
		applyTerm = pair.getKey();
		applyIdx = pair.getValue();
		lastSnapshotApplyIdx = pair.getValue();
		loadData();
		return 0;
	}

	/**
	 * 加载快照应用的索引位置与版本
	 * 
	 * @return 版本, 索引位置
	 */
	protected abstract Pair<Long, Integer> loadApplyTermAndIdx();

	/**
	 * 加载数据
	 */
	protected abstract void loadData();

	@Override
	public final int getApplyIdx() {
		return applyIdx;
	}

	/**
	 * 获取磁盘快照的applyIdx
	 *
	 * @return 快照的applyIdx
	 */
	@Override
	public int getSnapshotApplyIdx() {
		return lastSnapshotApplyIdx;
	}

	@Override
	public final Snapshot getSnapshot() {
		lock.lock();
		try {
			return new Snapshot(applyTerm, applyIdx, deepCopySnapshotData());
		} finally {
			lock.unlock();
		}
	}

	/**
	 * 获取快照副本
	 * 
	 * @return 快照副本
	 */
	public abstract byte[] deepCopySnapshotData();

	@Override
	public final Object apply(long logTerm, int logIdx, String command) {
		if (logIdx == applyIdx + 1) {
			lock.lock();
			try {
				this.applyTerm = logTerm;
				this.applyIdx = logIdx;
				if (Utils.NO_OP.equals(command) || Utils.SYNC.equals(command)) {
					return null;
				}
				return apply(getCommandSerializer().deserialize(command));
			} finally {
				lock.unlock();
			}
		} else {
			log.warn("apply failed, log index: {}, current apply index: {}", logIdx, this.applyIdx);
		}
		return null;
	}

	/**
	 * 获取命令解析器
	 * 
	 * @return 解析器
	 */
	public abstract CommandSerializer<T> getCommandSerializer();

	/**
	 * 应用命令
	 *
	 * @param command
	 *            命令
	 * @return 返回结果
	 */
	public abstract Object apply(T command);

	@Override
	public final void saveSnapshotToFile() {
		lock.lock();
		try {
			this.lastSnapshotApplyIdx = this.applyIdx;
			Snapshot snapshot = getSnapshot();
			saveSnapshotToFile(snapshot);
		} finally {
			lock.unlock();
		}
	}

	/**
	 * 保存快照到文件
	 *
	 * @param snapshot
	 *            快照
	 */
	public abstract void saveSnapshotToFile(Snapshot snapshot);

	@Override
	public final void saveSnapshot(long applyTerm, int applyIdx, byte[] data) {
		lock.lock();
		try {
			this.applyTerm = applyTerm;
			this.applyIdx = applyIdx;
			saveSnapshot(data);
			saveSnapshotToFile();
		} finally {
			lock.unlock();
		}
	}

	/**
	 * 保存快照到内存
	 *
	 * @param data
	 *            数据
	 */
	public abstract void saveSnapshot(byte[] data);

	public abstract String doValidateCommand(String command);

	/**
	 * 校验命令
	 *
	 * @param command
	 *            命令
	 * @return String
	 */
	@Override
	public String validateCommand(String command) {
		if (Utils.NO_OP.equals(command)) {
			return "";
		}
		return doValidateCommand(command);
	}
}
