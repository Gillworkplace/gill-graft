package com.gill.graft.entity;

import java.util.Arrays;

/**
 * ReplicateSnapshotParam
 *
 * @author gill
 * @version 2023/09/12
 **/
public class ReplicateSnapshotParam extends BaseParam {

	private int applyIdx;

	private long applyTerm;

	private byte[] data;

	public ReplicateSnapshotParam(int nodeId, long term, int applyIdx, long applyTerm, byte[] data) {
		super(nodeId, term);
		this.applyIdx = applyIdx;
		this.applyTerm = applyTerm;
		this.data = data;
	}

	public int getApplyIdx() {
		return applyIdx;
	}

	public long getApplyTerm() {
		return applyTerm;
	}

	public byte[] getData() {
		return data;
	}

	@Override
	public String toString() {
		return "ReplicateSnapshotParam{" + super.toString() + "applyIdx=" + applyIdx + ", applyTerm=" + applyTerm
				+ ", data=" + Arrays.toString(data) + '}';
	}

	public void setApplyIdx(int applyIdx) {
		this.applyIdx = applyIdx;
	}

	public void setApplyTerm(long applyTerm) {
		this.applyTerm = applyTerm;
	}

	public void setData(byte[] data) {
		this.data = data;
	}
}
