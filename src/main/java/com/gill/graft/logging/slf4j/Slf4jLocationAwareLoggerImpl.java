package com.gill.graft.logging.slf4j;

import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import org.slf4j.spi.LocationAwareLogger;

import com.gill.graft.logging.Log;

/**
 * Slf4jLocationAwareLoggerImpl
 *
 * @author gill
 * @version 2023/09/20
 **/
class Slf4jLocationAwareLoggerImpl implements Log {

	private static final Marker MARKER = MarkerFactory.getMarker("GRAFT");
	private static final String FQCN = Slf4jImpl.class.getName();
	private final LocationAwareLogger logger;

	Slf4jLocationAwareLoggerImpl(LocationAwareLogger logger) {
		this.logger = logger;
	}

	public boolean isDebugEnabled() {
		return this.logger.isDebugEnabled();
	}

	public boolean isTraceEnabled() {
		return this.logger.isTraceEnabled();
	}

	public void error(String s, Throwable e) {
		this.logger.log(MARKER, FQCN, 40, s, (Object[]) null, e);
	}

	public void error(String s) {
		this.logger.log(MARKER, FQCN, 40, s, (Object[]) null, (Throwable) null);
	}

	public void debug(String s) {
		this.logger.log(MARKER, FQCN, 10, s, (Object[]) null, (Throwable) null);
	}

	public void trace(String s) {
		this.logger.log(MARKER, FQCN, 0, s, (Object[]) null, (Throwable) null);
	}

	public void warn(String s) {
		this.logger.log(MARKER, FQCN, 30, s, (Object[]) null, (Throwable) null);
	}
}