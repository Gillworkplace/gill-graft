package com.gill.graft.config;

/**
 * RaftConfig
 *
 * @author gill
 * @version 2023/09/06
 **/
public class RaftConfig {

	private long heartbeatInterval = 100L;

	private long baseTimeoutInterval = 300L;

	private long checkTimeoutInterval = 100L;

	private long timeoutRandomFactor = 150;

	private int repairLength = 100;

	private LogConfig logConfig = new LogConfig();

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
