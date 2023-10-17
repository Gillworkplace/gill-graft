package com.gill.graft.entity;

import com.gill.graft.proto.Raft;
import com.gill.graft.proto.RaftConverter;

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

	/**
	 * encode
	 *
	 * @return ret
	 */
	public byte[] encode() {
		Raft.Reply baseReply = RaftConverter.buildReply(this);
		Raft.AppendLogReply reply = Raft.AppendLogReply.newBuilder().setReply(baseReply).setSyncSnapshot(syncSnapshot)
				.setCompareIdx(compareIdx).build();
		return reply.toByteArray();
	}

	/**
	 * decoder
	 *
	 * @param bytes
	 *            bytes
	 * @return ret
	 */
	public static AppendLogReply decode(byte[] bytes) {
		if(bytes == null || bytes.length == 0) {
			return new AppendLogReply(false, -1);
		}
		Raft.AppendLogReply reply = RaftConverter.parseFrom(bytes, Raft.AppendLogReply::parseFrom, "AppendLogReply");
		if (reply == null) {
			return new AppendLogReply(false, -1);
		}
		Raft.Reply baseReply = reply.getReply();
		return new AppendLogReply(baseReply.getSuccess(), baseReply.getTerm(), reply.getSyncSnapshot(),
				reply.getCompareIdx());
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

	public void setSyncSnapshot(boolean syncSnapshot) {
		this.syncSnapshot = syncSnapshot;
	}

	public void setCompareIdx(int compareIdx) {
		this.compareIdx = compareIdx;
	}
}
