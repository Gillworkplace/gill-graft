package com.gill.graft.entity;

/**
 * Reply
 *
 * @author gill
 * @version 2023/08/18
 **/
public class Reply {

	private final boolean success;

	private final long term;

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
}
