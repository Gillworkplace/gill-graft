package com.gill.graft.entity;

/**
 * TermParam
 *
 * @author gill
 * @version 2023/09/13
 **/
public abstract class BaseParam {

	private int nodeId;

	private long term;

	public BaseParam(int nodeId, long term) {
		this.nodeId = nodeId;
		this.term = term;
	}

	public int getNodeId() {
		return nodeId;
	}

	public long getTerm() {
		return term;
	}

	@Override
	public String toString() {
		return "BaseParam{" + "nodeId=" + nodeId + ", term=" + term + '}';
	}

	public void setNodeId(int nodeId) {
		this.nodeId = nodeId;
	}

	public void setTerm(long term) {
		this.term = term;
	}
}
