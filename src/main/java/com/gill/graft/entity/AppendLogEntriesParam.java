package com.gill.graft.entity;

import java.util.List;

import com.gill.graft.model.LogEntry;

/**
 * AppendLogEntriesParam
 *
 * @author gill
 * @version 2023/08/18
 **/
public class AppendLogEntriesParam extends BaseParam {

	private long preLogTerm;

	private int preLogIdx;

	private int commitIdx;

	private List<LogEntry> logs;

	public AppendLogEntriesParam(int nodeId, long term) {
		super(nodeId, term);
	}

	public AppendLogEntriesParam(int nodeId, long term, long preLogTerm, int preLogIdx, int commitIdx,
			List<LogEntry> logs) {
		super(nodeId, term);
		this.preLogTerm = preLogTerm;
		this.preLogIdx = preLogIdx;
		this.commitIdx = commitIdx;
		this.logs = logs;
	}

	public long getPreLogTerm() {
		return preLogTerm;
	}

	public int getPreLogIdx() {
		return preLogIdx;
	}

	public int getCommitIdx() {
		return commitIdx;
	}

	public List<LogEntry> getLogs() {
		return logs;
	}
}
