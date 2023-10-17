package com.gill.graft.entity;

import java.util.Arrays;

import com.gill.graft.proto.Raft;
import com.gill.graft.proto.RaftConverter;
import com.google.protobuf.ByteString;

/**
 * ReplicateSnapshotParam
 *
 * @author gill
 * @version 2023/09/12
 **/
public class ReplicateSnapshotParam extends BaseParam {

	private int applyIdx;

	private long applyTerm;

	private byte[] data;

	public ReplicateSnapshotParam(int nodeId, long term, int applyIdx, long applyTerm, byte[] data) {
		super(nodeId, term);
		this.applyIdx = applyIdx;
		this.applyTerm = applyTerm;
		this.data = data;
	}

	/**
	 * encode
	 *
	 * @return ret
	 */
	public byte[] encode() {
		Raft.BaseParam baseParam = RaftConverter.buildReply(this);
		Raft.ReplicateSnapshotParam rpcParam = Raft.ReplicateSnapshotParam.newBuilder().setBaseParam(baseParam)
				.setApplyIdx(applyIdx).setApplyTerm(applyTerm).setData(ByteString.copyFrom(data)).build();
		return rpcParam.toByteArray();
	}

	/**
	 * decoder
	 *
	 * @param bytes
	 *            bytes
	 * @return ret
	 */
	public static ReplicateSnapshotParam decode(byte[] bytes) {
		Raft.ReplicateSnapshotParam param = RaftConverter.parseFrom(bytes, Raft.ReplicateSnapshotParam::parseFrom,
				"ReplicateSnapshotParam");
		if (param == null) {
			return null;
		}
		Raft.BaseParam baseParam = param.getBaseParam();
		return new ReplicateSnapshotParam(baseParam.getNodeId(), baseParam.getTerm(), param.getApplyIdx(),
				param.getApplyTerm(), param.getData().toByteArray());
	}

	public int getApplyIdx() {
		return applyIdx;
	}

	public long getApplyTerm() {
		return applyTerm;
	}

	public byte[] getData() {
		return data;
	}

	@Override
	public String toString() {
		return "ReplicateSnapshotParam{" + super.toString() + "applyIdx=" + applyIdx + ", applyTerm=" + applyTerm
				+ ", data=" + Arrays.toString(data) + '}';
	}

	public void setApplyIdx(int applyIdx) {
		this.applyIdx = applyIdx;
	}

	public void setApplyTerm(long applyTerm) {
		this.applyTerm = applyTerm;
	}

	public void setData(byte[] data) {
		this.data = data;
	}
}
