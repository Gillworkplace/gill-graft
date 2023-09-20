package com.gill.graft.logging.log4j2;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.apache.logging.log4j.message.SimpleMessage;
import org.apache.logging.log4j.spi.AbstractLogger;
import org.apache.logging.log4j.spi.ExtendedLoggerWrapper;

import com.gill.graft.logging.Log;

/**
 * Log4j2AbstractLoggerImpl
 *
 * @author gill
 * @version 2023/09/20
 **/
public class Log4j2AbstractLoggerImpl implements Log {
	private static final Marker MARKER = MarkerManager.getMarker("MYBATIS");
	private static final String FQCN = Log4j2Impl.class.getName();
	private final ExtendedLoggerWrapper log;

	public Log4j2AbstractLoggerImpl(AbstractLogger abstractLogger) {
		this.log = new ExtendedLoggerWrapper(abstractLogger, abstractLogger.getName(),
				abstractLogger.getMessageFactory());
	}

	public boolean isDebugEnabled() {
		return this.log.isDebugEnabled();
	}

	public boolean isTraceEnabled() {
		return this.log.isTraceEnabled();
	}

	public void error(String s, Throwable e) {
		this.log.logIfEnabled(FQCN, Level.ERROR, MARKER, new SimpleMessage(s), e);
	}

	public void error(String s) {
		this.log.logIfEnabled(FQCN, Level.ERROR, MARKER, new SimpleMessage(s), (Throwable) null);
	}

	public void debug(String s) {
		this.log.logIfEnabled(FQCN, Level.DEBUG, MARKER, new SimpleMessage(s), (Throwable) null);
	}

	public void trace(String s) {
		this.log.logIfEnabled(FQCN, Level.TRACE, MARKER, new SimpleMessage(s), (Throwable) null);
	}

	public void warn(String s) {
		this.log.logIfEnabled(FQCN, Level.WARN, MARKER, new SimpleMessage(s), (Throwable) null);
	}
}
