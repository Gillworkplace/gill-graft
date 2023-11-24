package com.gill.graft.model;

/**
 * IntMapReply
 *
 * @author gill
 * @version 2023/09/18
 **/
public class ProposeReply {

	private Integer idx;

	private boolean success;

	private String exception = "";

	private Object data;

	public ProposeReply(int logIdx) {
		this.idx = logIdx;
		this.success = true;
	}

	public ProposeReply(String exception) {
		this.idx = -1;
		this.exception = exception;
		this.success = false;
	}

	public Integer getIdx() {
		return idx;
	}

	public boolean isSuccess() {
		return success;
	}

	public void setSuccess(boolean success) {
		this.success = success;
	}

	public String getException() {
		return exception;
	}

	public void setException(String exception) {
		this.exception = exception;
	}

	public Object getData() {
		return data;
	}

	public void setData(Object data) {
		this.data = data;
	}

	@Override
	public String toString() {
		return "ProposeReply{" + "idx=" + idx + ", exception='" + exception + '\'' + ", data=" + data + '}';
	}
}
