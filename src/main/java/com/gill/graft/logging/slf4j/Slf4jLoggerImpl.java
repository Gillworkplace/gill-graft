package com.gill.graft.logging.slf4j;

import org.slf4j.Logger;

import com.gill.graft.logging.Log;

/**
 * Slf4jLoggerImpl
 *
 * @author gill
 * @version 2023/09/20
 **/
class Slf4jLoggerImpl implements Log {
	private final Logger log;

	public Slf4jLoggerImpl(Logger logger) {
		this.log = logger;
	}

	public boolean isDebugEnabled() {
		return this.log.isDebugEnabled();
	}

	public boolean isTraceEnabled() {
		return this.log.isTraceEnabled();
	}

	public void error(String s, Throwable e) {
		this.log.error(s, e);
	}

	public void error(String s) {
		this.log.error(s);
	}

	public void debug(String s) {
		this.log.debug(s);
	}

	public void trace(String s) {
		this.log.trace(s);
	}

	public void warn(String s) {
		this.log.warn(s);
	}
}
