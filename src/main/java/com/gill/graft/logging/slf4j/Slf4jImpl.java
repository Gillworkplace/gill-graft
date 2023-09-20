package com.gill.graft.logging.slf4j;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.spi.LocationAwareLogger;

import com.gill.graft.logging.Log;

/**
 * Slf4jImpl
 *
 * @author gill
 * @version 2023/09/20
 **/
public class Slf4jImpl implements Log {
	private Log log;

	public Slf4jImpl(String clazz) {
		Logger logger = LoggerFactory.getLogger(clazz);
		if (logger instanceof LocationAwareLogger) {
			try {
				logger.getClass().getMethod("log", Marker.class, String.class, Integer.TYPE, String.class,
						Object[].class, Throwable.class);
				this.log = new Slf4jLocationAwareLoggerImpl((LocationAwareLogger) logger);
				return;
			} catch (NoSuchMethodException | SecurityException var4) {
			}
		}

		this.log = new Slf4jLoggerImpl(logger);
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