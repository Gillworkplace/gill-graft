package com.gill.graft.apis;

import java.util.Collections;
import java.util.List;

import com.gill.graft.model.LogEntry;

/**
 * EmptyLogDb
 *
 * @author gill
 * @version 2023/09/11
 **/
public class EmptyLogStorage implements LogStorage {

	@Override
	public List<LogEntry> loadFromApplyIdx(int n, int applyIdx) {
		return Collections.emptyList();
	}

	@Override
	public void write(LogEntry logEntry) {

	}

	@Override
	public void rebuild(LogEntry logEntry) {

	}

	@Override
	public List<LogEntry> read(int start, int len) {
		return Collections.emptyList();
	}
}
