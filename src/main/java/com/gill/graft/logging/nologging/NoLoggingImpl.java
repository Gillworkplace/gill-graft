package com.gill.graft.logging.nologging;

import com.gill.graft.logging.Log;

/**
 * NoLoggingImpl
 *
 * @author gill
 * @version 2023/09/20
 **/
public class NoLoggingImpl implements Log {
	public NoLoggingImpl(String clazz) {
	}

	public boolean isDebugEnabled() {
		return false;
	}

	public boolean isTraceEnabled() {
		return false;
	}

	public void error(String s, Throwable e) {
	}

	public void error(String s) {
	}

	public void debug(String s) {
	}

	public void trace(String s) {
	}

	public void warn(String s) {
	}
}
