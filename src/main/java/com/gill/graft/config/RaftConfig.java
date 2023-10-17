package com.gill.graft.config;

/**
 * RaftConfig
 *
 * @author gill
 * @version 2023/09/06
 **/
public class RaftConfig {

	private int port = 8160;

	private long connectTimeout = 5 * 1000L;

	private long requestTimeout = 30 * 1000L;

	private long heartbeatInterval = 100L;

	private long baseTimeoutInterval = 300L;

	private long checkTimeoutInterval = 100L;

	private long timeoutRandomFactor = 150;

	private int repairLength = 100;

	private long snapshotPersistedInterval = 5L * 60 * 1000;

	private LogConfig logConfig = new LogConfig();

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public long getConnectTimeout() {
		return connectTimeout;
	}

	public void setConnectTimeout(long connectTimeout) {
		this.connectTimeout = connectTimeout;
	}

	public long getRequestTimeout() {
		return requestTimeout;
	}

	public void setRequestTimeout(long requestTimeout) {
		this.requestTimeout = requestTimeout;
	}

	public long getHeartbeatInterval() {
		return heartbeatInterval;
	}

	public void setHeartbeatInterval(long heartbeatInterval) {
		this.heartbeatInterval = heartbeatInterval;
	}

	public long getBaseTimeoutInterval() {
		return baseTimeoutInterval;
	}

	public void setBaseTimeoutInterval(long baseTimeoutInterval) {
		this.baseTimeoutInterval = baseTimeoutInterval;
	}

	public long getCheckTimeoutInterval() {
		return checkTimeoutInterval;
	}

	public void setCheckTimeoutInterval(long checkTimeoutInterval) {
		this.checkTimeoutInterval = checkTimeoutInterval;
	}

	public long getTimeoutRandomFactor() {
		return timeoutRandomFactor;
	}

	public void setTimeoutRandomFactor(long timeoutRandomFactor) {
		this.timeoutRandomFactor = timeoutRandomFactor;
	}

	public int getRepairLength() {
		return repairLength;
	}

	public void setRepairLength(int repairLength) {
		this.repairLength = repairLength;
	}

	public long getSnapshotPersistedInterval() {
		return snapshotPersistedInterval;
	}

	public void setSnapshotPersistedInterval(long snapshotPersistedInterval) {
		this.snapshotPersistedInterval = snapshotPersistedInterval;
	}

	public LogConfig getLogConfig() {
		return logConfig;
	}

	public void setLogConfig(LogConfig logConfig) {
		this.logConfig = logConfig;
	}

	@Override
	public String toString() {
		return "RaftConfig{" + "heartbeatInterval=" + heartbeatInterval + ", baseTimeoutInterval=" + baseTimeoutInterval
				+ ", checkTimeoutInterval=" + checkTimeoutInterval + ", timeoutRandomFactor=" + timeoutRandomFactor
				+ ", repairLength=" + repairLength + ", logConfig=" + logConfig + '}';
	}

	public static class LogConfig {

		private int loadLen = 30;

		public int getLoadLen() {
			return loadLen;
		}

		public void setLoadLen(int loadLen) {
			this.loadLen = loadLen;
		}

		@Override
		public String toString() {
			return "LogConfig{" + "loadLen=" + loadLen + '}';
		}
	}
}
