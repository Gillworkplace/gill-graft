package com.gill.graft.machine;

import java.util.concurrent.CountDownLatch;

/**
 * RaftEventParams
 *
 * @author gill
 * @version 2023/09/04
 **/
public class RaftEventParams {

	private long term;

	private boolean sync;

	private CountDownLatch latch = new CountDownLatch(1);

	public RaftEventParams(long term) {
		this.term = term;
	}

	public RaftEventParams(long term, boolean sync) {
		this.term = term;
		this.sync = sync;
	}

	public long getTerm() {
		return term;
	}

	public boolean isSync() {
		return sync;
	}

	public CountDownLatch getLatch() {
		return latch;
	}
}
