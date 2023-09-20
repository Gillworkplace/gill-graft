package com.gill.graft.model;

/**
 * Snapshot
 *
 * @author gill
 * @version 2023/09/12
 **/
public class Snapshot {

	private long applyTerm;

	private int applyIdx;

	private byte[] data;

	public long getApplyTerm() {
		return applyTerm;
	}

	public int getApplyIdx() {
		return applyIdx;
	}

	public byte[] getData() {
		return data;
	}

	public Snapshot(long applyTerm, int applyIdx, byte[] data) {
		this.applyTerm = applyTerm;
		this.applyIdx = applyIdx;
		this.data = data;
	}
}
