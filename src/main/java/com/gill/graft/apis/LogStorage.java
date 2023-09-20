package com.gill.graft.apis;

import java.util.List;

import com.gill.graft.model.LogEntry;

/**
 * LogDb
 *
 * @author gill
 * @version 2023/09/11
 **/
public interface LogStorage {

	/**
	 * 读取最新的n条日志
	 * 
	 * @param n
	 *            n
	 * @param applyIdx
	 *            快照应用的最大日志ID
	 * @return 日志
	 */
	List<LogEntry> loadFromApplyIdx(int n, int applyIdx);

	/**
	 * 写日志
	 * 
	 * @param logEntry
	 *            日志
	 */
	void write(LogEntry logEntry);

	/**
	 * 重写日志
	 *
	 * @param logEntry
	 *            日志
	 */
	void rebuild(LogEntry logEntry);

	/**
	 * 读日志
	 * 
	 * @param start
	 *            索引
	 * @param len
	 *            个数
	 */
	List<LogEntry> read(int start, int len);
}
