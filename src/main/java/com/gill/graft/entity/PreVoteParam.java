package com.gill.graft.entity;

import com.gill.graft.proto.Raft;
import com.gill.graft.proto.RaftConverter;

/**
 * PreVoteParam
 *
 * @author gill
 * @version 2023/08/18
 **/
public class PreVoteParam extends BaseParam {

	private long lastLogTerm;

	private int lastLogIdx;

	public PreVoteParam(int nodeId, long term, long lastLogTerm, int lastLogIdx) {
		super(nodeId, term);
		this.lastLogTerm = lastLogTerm;
		this.lastLogIdx = lastLogIdx;
	}

	/**
	 * encode
	 * 
	 * @return ret
	 */
	public byte[] encode() {
		Raft.BaseParam baseParam = RaftConverter.buildReply(this);
		Raft.PreVoteParam rpcParam = Raft.PreVoteParam.newBuilder().setBaseParam(baseParam).setLastLogTerm(lastLogTerm)
				.setLastLogIdx(lastLogIdx).build();
		return rpcParam.toByteArray();
	}

	/**
	 * decoder
	 * 
	 * @param bytes
	 *            bytes
	 * @return ret
	 */
	public static PreVoteParam decode(byte[] bytes) {
		Raft.PreVoteParam param = RaftConverter.parseFrom(bytes, Raft.PreVoteParam::parseFrom, "PreVoteParam");
		if (param == null) {
			return null;
		}
		Raft.BaseParam baseParam = param.getBaseParam();
		return new PreVoteParam(baseParam.getNodeId(), baseParam.getTerm(), param.getLastLogTerm(),
				param.getLastLogIdx());
	}

	public long getLastLogTerm() {
		return lastLogTerm;
	}

	public int getLastLogIdx() {
		return lastLogIdx;
	}

	@Override
	public String toString() {
		return "PreVoteParam{" + super.toString() + "lastLogTerm=" + lastLogTerm + ", lastLogIdx=" + lastLogIdx + '}';
	}

	public void setLastLogTerm(long lastLogTerm) {
		this.lastLogTerm = lastLogTerm;
	}

	public void setLastLogIdx(int lastLogIdx) {
		this.lastLogIdx = lastLogIdx;
	}
}
