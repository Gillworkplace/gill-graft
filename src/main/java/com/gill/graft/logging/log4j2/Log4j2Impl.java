package com.gill.graft.logging.log4j2;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.spi.AbstractLogger;

import com.gill.graft.logging.Log;

/**
 * Log4j2Impl
 *
 * @author gill
 * @version 2023/09/20
 **/
public class Log4j2Impl implements Log {
	private final Log log;

	public Log4j2Impl(String clazz) {
		Logger logger = LogManager.getLogger(clazz);
		if (logger instanceof AbstractLogger) {
			this.log = new Log4j2AbstractLoggerImpl((AbstractLogger) logger);
		} else {
			this.log = new Log4j2LoggerImpl(logger);
		}

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