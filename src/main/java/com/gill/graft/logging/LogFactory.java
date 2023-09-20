package com.gill.graft.logging;

import java.lang.reflect.Constructor;

import com.gill.graft.logging.log4j2.Log4j2Impl;
import com.gill.graft.logging.nologging.NoLoggingImpl;
import com.gill.graft.logging.slf4j.Slf4jImpl;

/**
 * LogFactory
 *
 * @author gill
 * @version 2023/09/20
 **/
public final class LogFactory {

	private static Constructor<? extends Log> logConstructor;

	private LogFactory() {
	}

	public static Log getLog(Class<?> clazz) {
		return getLog(clazz.getName());
	}

	public static Log getLog(String logger) {
		try {
			return (Log) logConstructor.newInstance(logger);
		} catch (Throwable var2) {
			throw new LogException("Error creating logger for logger " + logger + ".  Cause: " + var2, var2);
		}
	}

	public static synchronized void useSlf4jLogging() {
		setImplementation(Slf4jImpl.class);
	}

	public static synchronized void useLog4J2Logging() {
		setImplementation(Log4j2Impl.class);
	}

	public static synchronized void useNoLogging() {
		setImplementation(NoLoggingImpl.class);
	}

	private static void tryImplementation(Runnable runnable) {
		if (logConstructor == null) {
			try {
				runnable.run();
			} catch (Throwable var2) {
			}
		}

	}

	private static void setImplementation(Class<? extends Log> implClass) {
		try {
			Constructor<? extends Log> candidate = implClass.getConstructor(String.class);
			Log log = (Log) candidate.newInstance(LogFactory.class.getName());
			if (log.isDebugEnabled()) {
				log.debug("Logging initialized using '" + implClass + "' adapter.");
			}

			logConstructor = candidate;
		} catch (Throwable var3) {
			throw new LogException("Error setting Log implementation.  Cause: " + var3, var3);
		}
	}

	static {
		tryImplementation(LogFactory::useSlf4jLogging);
		tryImplementation(LogFactory::useLog4J2Logging);
		tryImplementation(LogFactory::useNoLogging);
	}
}