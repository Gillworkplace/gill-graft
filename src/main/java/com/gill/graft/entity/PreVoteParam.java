package com.gill.graft.entity;

/**
 * PreVoteParam
 *
 * @author gill
 * @version 2023/08/18
 **/
public class PreVoteParam extends BaseParam {

	private final long lastLogTerm;

	private final int lastLogIdx;

	public PreVoteParam(int nodeId, long term, long lastLogTerm, int lastLogIdx) {
		super(nodeId, term);
		this.lastLogTerm = lastLogTerm;
		this.lastLogIdx = lastLogIdx;
	}

	public long getLastLogTerm() {
		return lastLogTerm;
	}

	public int getLastLogIdx() {
		return lastLogIdx;
	}

	@Override
	public String toString() {
		return "PreVoteParam{" + super.toString() + "lastLogTerm=" + lastLogTerm + ", lastLogIdx=" + lastLogIdx + '}';
	}
}
