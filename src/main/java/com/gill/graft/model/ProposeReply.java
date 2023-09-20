package com.gill.graft.model;

/**
 * IntMapReply
 *
 * @author gill
 * @version 2023/09/18
 **/
public class ProposeReply {

	private Integer idx;

	private Object data;

	public ProposeReply() {
	}

	public ProposeReply(Integer idx) {
		this.idx = idx;
	}

	public ProposeReply(Integer idx, Object data) {
		this.idx = idx;
		this.data = data;
	}

	public Integer getIdx() {
		return idx;
	}

	public void setIdx(Integer idx) {
		this.idx = idx;
	}

	public Object getData() {
		return data;
	}

	public void setData(Object data) {
		this.data = data;
	}

	@Override
	public String toString() {
		return "ProposeReply{" + "idx=" + idx + ", data=" + data + '}';
	}
}
