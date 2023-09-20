package com.gill.graft.model;

/**
 * LogEntity
 *
 * @author gill
 * @version 2023/09/07
 **/
public class LogEntry {

	private final int index;

	private final long term;

	private final String command;

	public int getIndex() {
		return index;
	}

	public long getTerm() {
		return term;
	}

	public String getCommand() {
		return command;
	}

	public LogEntry(int index, long term, String command) {
		this.index = index;
		this.term = term;
		this.command = command;
	}

	@Override
	public String toString() {
		return "LogEntry{" + "index=" + index + ", term=" + term + ", command='" + command + '\'' + '}';
	}
}
