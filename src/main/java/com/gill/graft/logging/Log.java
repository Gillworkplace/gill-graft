package com.gill.graft.logging;

/**
 * Log
 *
 * @author gill
 * @version 2023/09/20
 **/
public interface Log {
	boolean isDebugEnabled();

	boolean isTraceEnabled();

	void error(String var1, Throwable var2);

	void error(String var1);

	void debug(String var1);

	void trace(String var1);

	void warn(String var1);
}