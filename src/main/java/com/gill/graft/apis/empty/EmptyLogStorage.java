package com.gill.graft.apis.empty;

import java.util.Collections;
import java.util.List;

import com.gill.graft.apis.LogStorage;
import com.gill.graft.model.LogEntry;

/**
 * EmptyLogDb
 *
 * @author gill
 * @version 2023/09/11
 **/
public class EmptyLogStorage implements LogStorage {

	public static final EmptyLogStorage INSTANCE = new EmptyLogStorage();

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
