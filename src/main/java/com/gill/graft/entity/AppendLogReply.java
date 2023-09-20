package com.gill.graft.entity;

/**
 * AppendLogReply
 *
 * @author gill
 * @version 2023/09/11
 **/
public class AppendLogReply extends Reply {

	private boolean syncSnapshot = false;

	private int compareIdx = -1;

	public AppendLogReply(boolean success, long term) {
		super(success, term);
	}

	public AppendLogReply(boolean success, long term, boolean syncSnapshot) {
		super(success, term);
		this.syncSnapshot = syncSnapshot;
	}

	public AppendLogReply(boolean success, long term, boolean syncSnapshot, int compareIdx) {
		super(success, term);
		this.syncSnapshot = syncSnapshot;
		this.compareIdx = compareIdx;
	}

	public boolean isSyncSnapshot() {
		return syncSnapshot;
	}

	public int getCompareIdx() {
		return compareIdx;
	}

	@Override
	public String toString() {
		return "AppendLogReply{" + super.toString() + "syncSnapshot=" + syncSnapshot + ", compareIdx=" + compareIdx
				+ '}';
	}
}
