package com.gill.graft.logging.log4j2;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;

import com.gill.graft.logging.Log;

/**
 * Log4j2LoggerImpl
 *
 * @author gill
 * @version 2023/09/20
 **/
public class Log4j2LoggerImpl implements Log {
	private static final Marker MARKER = MarkerManager.getMarker("MYBATIS");
	private final Logger log;

	public Log4j2LoggerImpl(Logger logger) {
		this.log = logger;
	}

	public boolean isDebugEnabled() {
		return this.log.isDebugEnabled();
	}

	public boolean isTraceEnabled() {
		return this.log.isTraceEnabled();
	}

	public void error(String s, Throwable e) {
		this.log.error(MARKER, s, e);
	}

	public void error(String s) {
		this.log.error(MARKER, s);
	}

	public void debug(String s) {
		this.log.debug(MARKER, s);
	}

	public void trace(String s) {
		this.log.trace(MARKER, s);
	}

	public void warn(String s) {
		this.log.warn(MARKER, s);
	}
}