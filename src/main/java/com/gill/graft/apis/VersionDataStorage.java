package com.gill.graft.apis;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gill.graft.common.Utils;
import com.gill.graft.model.Snapshot;

/**
 * VersionDataStorage
 *
 * @author gill
 * @version 2023/09/18
 **/
public abstract class VersionDataStorage<T> implements DataStorage {

	private static final Logger log = LoggerFactory.getLogger(VersionDataStorage.class);

	private long applyTerm = 0;

	private int applyIdx = 0;

	private final Lock lock = new ReentrantLock();

	@Override
	public int getApplyIdx() {
		return applyIdx;
	}

	@Override
	public final Snapshot getSnapshot() {
		lock.lock();
		try {
			return new Snapshot(applyTerm, applyIdx, getSnapshotData());
		} finally {
			lock.unlock();
		}
	}

	/**
	 * 获取快照副本
	 * 
	 * @return 快照副本
	 */
	public abstract byte[] getSnapshotData();

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
			log.warn("discontinuous log index: {}, current index: {}", logIdx, this.applyIdx);
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
			saveSnapshotToFile(getSnapshot());
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
}
