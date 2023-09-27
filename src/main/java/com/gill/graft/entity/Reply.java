package com.gill.graft.entity;

/**
 * Reply
 *
 * @author gill
 * @version 2023/08/18
 **/
public class Reply {

	private boolean success;

	private long term;

	public Reply(boolean success, long term) {
		this.success = success;
		this.term = term;
	}

	public boolean isSuccess() {
		return success;
	}

	public long getTerm() {
		return term;
	}

	@Override
	public String toString() {
		return "Reply{" + "success=" + success + ", term=" + term + '}';
	}

	public void setSuccess(boolean success) {
		this.success = success;
	}

	public void setTerm(long term) {
		this.term = term;
	}
}
