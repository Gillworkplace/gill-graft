package com.gill.graft.entity;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

import com.gill.graft.model.LogEntry;
import com.gill.graft.proto.Raft;
import com.gill.graft.proto.RaftConverter;
import com.google.protobuf.ByteString;

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

	public AppendLogEntriesParam(int nodeId, long term, int commitIdx) {
		super(nodeId, term);
		this.commitIdx = commitIdx;
	}

	public AppendLogEntriesParam(int nodeId, long term, long preLogTerm, int preLogIdx, int commitIdx,
			List<LogEntry> logs) {
		super(nodeId, term);
		this.preLogTerm = preLogTerm;
		this.preLogIdx = preLogIdx;
		this.commitIdx = commitIdx;
		this.logs = logs;
	}

	/**
	 * encode
	 *
	 * @return ret
	 */
	public byte[] encode() {
		Raft.BaseParam baseParam = RaftConverter.buildReply(this);
		Raft.AppendLogEntriesParam.Builder builder = Raft.AppendLogEntriesParam.newBuilder().setBaseParam(baseParam)
				.setPreLogIdx(preLogIdx).setPreLogTerm(preLogTerm).setCommitIdx(commitIdx);
		if (logs != null) {
			for (LogEntry logEntry : logs) {
				builder.addLogs(Raft.AppendLogEntriesParam.LogEntry.newBuilder().setIndex(logEntry.getIndex())
						.setTerm(logEntry.getTerm())
						.setCommand(ByteString.copyFrom(logEntry.getCommand().getBytes(StandardCharsets.UTF_8))));
			}
		}
		Raft.AppendLogEntriesParam rpcParam = builder.build();
		return rpcParam.toByteArray();
	}

	/**
	 * decoder
	 *
	 * @param bytes
	 *            bytes
	 * @return ret
	 */
	public static AppendLogEntriesParam decode(byte[] bytes) {
		Raft.AppendLogEntriesParam param = RaftConverter.parseFrom(bytes, Raft.AppendLogEntriesParam::parseFrom,
				"AppendLogEntriesParam");
		if (param == null) {
			return null;
		}
		Raft.BaseParam baseParam = param.getBaseParam();
		List<LogEntry> logEntries = param.getLogsList().stream().map(LogEntry::fromProto).collect(Collectors.toList());
		return new AppendLogEntriesParam(baseParam.getNodeId(), baseParam.getTerm(), param.getPreLogTerm(),
				param.getPreLogIdx(), param.getCommitIdx(), logEntries);
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

	public void setPreLogTerm(long preLogTerm) {
		this.preLogTerm = preLogTerm;
	}

	public void setPreLogIdx(int preLogIdx) {
		this.preLogIdx = preLogIdx;
	}

	public void setCommitIdx(int commitIdx) {
		this.commitIdx = commitIdx;
	}

	public void setLogs(List<LogEntry> logs) {
		this.logs = logs;
	}
}
