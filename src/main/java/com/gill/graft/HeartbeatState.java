package com.gill.graft;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.util.Pair;

/**
 * HeartbeatState
 *
 * @author gill
 * @version 2023/09/06
 **/
public class HeartbeatState {

	private static final Logger log = LoggerFactory.getLogger(HeartbeatState.class);

	private long lastHeartbeatTerm;

	private long lastHeartbeatTimestamp;

	public HeartbeatState(long lastHeartbeatTerm, long lastHeartbeatTimestamp) {
		this.lastHeartbeatTerm = lastHeartbeatTerm;
		this.lastHeartbeatTimestamp = lastHeartbeatTimestamp;
	}

	/**
	 * set
	 * 
	 * @param term
	 *            上次心跳收到的任期
	 * @param heartbeatTimestamp
	 *            上次收到心跳的时间
	 */
	public synchronized void set(long term, long heartbeatTimestamp) {
		if (term < this.lastHeartbeatTerm) {
			log.debug("discard heartbeat");
			return;
		}
		this.lastHeartbeatTerm = term;
		this.lastHeartbeatTimestamp = Math.max(heartbeatTimestamp, this.lastHeartbeatTimestamp);
	}

	/**
	 * get
	 * 
	 * @return lastHeartbeatTerm, lastHeartbeatTimestamp
	 */
	public synchronized Pair<Long, Long> get() {
		return new Pair<>(this.lastHeartbeatTerm, this.lastHeartbeatTimestamp);
	}

	@Override
	public String toString() {
		return "HeartbeatState{" + "lastHeartbeatTerm=" + lastHeartbeatTerm + ", lastHeartbeatTimestamp="
				+ lastHeartbeatTimestamp + '}';
	}
}
