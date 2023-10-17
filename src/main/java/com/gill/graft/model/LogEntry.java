package com.gill.graft.model;

import java.nio.charset.StandardCharsets;

import com.gill.graft.proto.Raft;
import com.google.protobuf.ByteString;

/**
 * LogEntity
 *
 * @author gill
 * @version 2023/09/07
 **/
public class LogEntry {

	private int index;

	private long term;

	private String command;

	public Raft.AppendLogEntriesParam.LogEntry toProto() {
		return Raft.AppendLogEntriesParam.LogEntry.newBuilder().setIndex(index).setTerm(term)
				.setCommand(ByteString.copyFrom(command, StandardCharsets.UTF_8)).build();
	}

	public static LogEntry fromProto(Raft.AppendLogEntriesParam.LogEntry logEntry) {
		return new LogEntry(logEntry.getIndex(), logEntry.getTerm(),
				logEntry.getCommand().toString(StandardCharsets.UTF_8));
	}

	public int getIndex() {
		return index;
	}

	public long getTerm() {
		return term;
	}

	public String getCommand() {
		return command;
	}

	public void setIndex(int index) {
		this.index = index;
	}

	public void setTerm(long term) {
		this.term = term;
	}

	public void setCommand(String command) {
		this.command = command;
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
