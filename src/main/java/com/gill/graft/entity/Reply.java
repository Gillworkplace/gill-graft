package com.gill.graft.entity;

import com.gill.graft.proto.Raft;
import com.gill.graft.proto.RaftConverter;

/**
 * Reply
 *
 * @author gill
 * @version 2023/08/18
 **/
public class Reply {

	private boolean success;

	private long term;

	public Reply(boolean success, long term) {
		this.success = success;
		this.term = term;
	}

	/**
	 * encode
	 *
	 * @return ret
	 */
	public byte[] encode() {
		Raft.Reply reply = Raft.Reply.newBuilder().setSuccess(success).setTerm(term).build();
		return reply.toByteArray();
	}

	/**
	 * decoder
	 *
	 * @param bytes
	 *            bytes
	 * @return ret
	 */
	public static Reply decode(byte[] bytes) {
		if(bytes == null || bytes.length == 0) {
			return new Reply(false, -1);
		}
		Raft.Reply reply = RaftConverter.parseFrom(bytes, Raft.Reply::parseFrom, "Reply");
		if (reply == null) {
			return new Reply(false, -1);
		}
		return new Reply(reply.getSuccess(), reply.getTerm());
	}

	public boolean isSuccess() {
		return success;
	}

	public long getTerm() {
		return term;
	}

	@Override
	public String toString() {
		return "Reply{" + "success=" + success + ", term=" + term + '}';
	}

	public void setSuccess(boolean success) {
		this.success = success;
	}

	public void setTerm(long term) {
		this.term = term;
	}
}
