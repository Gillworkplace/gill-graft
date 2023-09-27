package com.gill.graft.entity;

/**
 * RequestVoteParam
 *
 * @author gill
 * @version 2023/08/18
 **/
public class RequestVoteParam extends BaseParam {

	private long lastLogTerm;

	private int lastLogIdx;

	public RequestVoteParam(int nodeId, long term, long lastLogTerm, int lastLogIdx) {
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
		return "RequestVoteParam{" + super.toString() + "lastLogTerm=" + lastLogTerm + ", lastLogIdx=" + lastLogIdx
				+ '}';
	}

	public void setLastLogTerm(long lastLogTerm) {
		this.lastLogTerm = lastLogTerm;
	}

	public void setLastLogIdx(int lastLogIdx) {
		this.lastLogIdx = lastLogIdx;
	}
}
